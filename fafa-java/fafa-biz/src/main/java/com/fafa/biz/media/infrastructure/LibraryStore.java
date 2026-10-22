package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.domain.LibraryModels.*;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.common.error.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 媒体整理查询及持久化，不参与外部对象操作。 */
@Repository
public class LibraryStore {
    private final JdbcTemplate jdbc; private final MediaStore media;
    public LibraryStore(JdbcTemplate jdbc,MediaStore media) { this.jdbc=jdbc;this.media=media; }
    public List<AssetView> list(String owner,LibraryQuery q,int limit) {
        StringBuilder sql=new StringBuilder("SELECT m.id FROM media_asset m JOIN pet p ON p.id=m.pet_id WHERE m.owner_user_id=? AND m.state='READY' AND p.status IN ('ACTIVE','ARCHIVED') AND m.id>?");
        List<Object> args=new ArrayList<>(List.of(owner,q.cursor()==null?"":q.cursor()));
        if(q.petId()!=null) { sql.append(" AND m.pet_id=?");args.add(q.petId()); }
        if(q.type()!=null) { sql.append(" AND m.media_type=?");args.add(q.type().name()); }
        else sql.append(" AND m.media_type IN ('IMAGE','VIDEO')");
        if(q.albumId()!=null) { sql.append(" AND EXISTS(SELECT 1 FROM album_item a WHERE a.media_id=m.id AND a.album_id=?)");args.add(q.albumId()); }
        if(q.tag()!=null) { sql.append(" AND EXISTS(SELECT 1 FROM media_tag t WHERE t.media_id=m.id AND t.tag=?)");args.add(q.tag()); }
        if(q.q()!=null && !q.q().isBlank()) {
            sql.append(" AND (m.title LIKE ? ESCAPE '!' OR m.description LIKE ? ESCAPE '!')");
            String value="%"+q.q().replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(value);args.add(value);
        }
        if(q.from()!=null) { sql.append(" AND m.captured_at>=?");args.add(time(q.from())); }
        if(q.to()!=null) { sql.append(" AND m.captured_at<?");args.add(time(q.to())); }
        sql.append(" ORDER BY m.id LIMIT ?");args.add(limit);
        return jdbc.query(sql.toString(),(r,n)->r.getString(1),args.toArray()).stream().map(media::asset).toList();
    }
    public void metadata(String id,MetadataChange body) {
        jdbc.update("UPDATE media_asset SET title=?,description=?,version=version+1 WHERE id=?",body.title().strip(),body.description(),id);
        if(body.capturedAt()!=null) jdbc.update("UPDATE media_asset SET captured_at=?,captured_at_source='USER' WHERE id=?",time(body.capturedAt()),id);
        jdbc.update("DELETE FROM media_tag WHERE media_id=?",id);
        if(body.petId()!=null) {
            jdbc.update("UPDATE media_asset SET pet_id=? WHERE id=?",body.petId(),id);
            jdbc.update("UPDATE upload_session SET pet_id=? WHERE id=?",body.petId(),id);
        }
        body.tags().stream().map(String::strip).distinct().forEach(tag->jdbc.update("INSERT INTO media_tag(media_id,tag) VALUES(?,?)",id,tag));
    }
    public AlbumView album(String owner,String id) {
        var rows=jdbc.query("SELECT a.*,(SELECT COUNT(*) FROM album_item i WHERE i.album_id=a.id) item_count FROM album a WHERE a.id=? AND a.owner_user_id=?",
            (r,n)->new AlbumView(r.getString("id"),r.getString("name"),r.getString("description"),r.getLong("version"),r.getLong("item_count")),id,owner);
        if(rows.isEmpty()) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return rows.getFirst();
    }
    public List<AlbumView> albums(String owner,String cursor,int limit) {
        return jdbc.query("SELECT id FROM album WHERE owner_user_id=? AND id>? ORDER BY id LIMIT ?",(r,n)->r.getString(1),
            owner,cursor==null?"":cursor,limit).stream().map(id->album(owner,id)).toList();
    }
    public void createAlbum(String owner,String id,AlbumCreate body) {
        jdbc.update("INSERT INTO album(id,owner_user_id,name,description) VALUES(?,?,?,?)",id,owner,body.name().strip(),body.description());
    }
    public void updateAlbum(String id,AlbumChange body) {
        jdbc.update("UPDATE album SET name=?,description=?,version=version+1 WHERE id=?",body.name().strip(),body.description(),id);
    }
    public void deleteAlbum(String id) { jdbc.update("DELETE FROM album_item WHERE album_id=?",id);jdbc.update("DELETE FROM album WHERE id=?",id); }
    public void membership(String album,String id,boolean add) {
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM album_item WHERE album_id=? AND media_id=?",Integer.class,album,id);
        if(add && count==0) jdbc.update("INSERT INTO album_item(album_id,media_id) VALUES(?,?)",album,id);
        if(!add && count>0) jdbc.update("DELETE FROM album_item WHERE album_id=? AND media_id=?",album,id);
        if((add&&count==0)||(!add&&count>0)) jdbc.update("UPDATE album SET version=version+1 WHERE id=?",album);
    }
    public DeletionImpact impact(AssetView a) {
        return new DeletionImpact(a.id(),a.version(),
            jdbc.queryForObject("SELECT COUNT(*) FROM media_binding WHERE media_id=?",Long.class,a.id()),
            jdbc.queryForObject("SELECT COUNT(*) FROM album_item WHERE media_id=?",Long.class,a.id()),
            jdbc.queryForObject("SELECT COUNT(*) FROM media_reference WHERE media_id=?",Long.class,a.id()),a.chargedBytes());
    }
    public void deleting(AssetView a,Instant now) {
        jdbc.update("UPDATE pet SET version=version+1,updated_at=? WHERE id IN(SELECT pet_id FROM media_binding WHERE media_id=?)",time(now),a.id());
        jdbc.update("DELETE FROM media_binding WHERE media_id=?",a.id());
        jdbc.update("UPDATE album SET version=version+1 WHERE id IN(SELECT album_id FROM album_item WHERE media_id=?)",a.id());
        jdbc.update("DELETE FROM album_item WHERE media_id=?",a.id());
        jdbc.update("DELETE FROM media_reference WHERE media_id=?",a.id());
        jdbc.update("UPDATE media_asset SET state='DELETION_PENDING',version=version+1 WHERE id=?",a.id());
        media.state(a.id(),State.DELETION_PENDING,a.id());
    }
    public List<String> pendingDeletes() {
        return jdbc.query("SELECT id FROM media_asset WHERE state='DELETION_PENDING' ORDER BY id LIMIT 50",(r,n)->r.getString(1));
    }
    public void deleted(String id) {
        jdbc.update("UPDATE media_asset SET state='DELETED',version=version+1 WHERE id=?",id);
        media.state(id,State.DELETED,id);
    }
    public static LocalDateTime time(Instant value) { return LocalDateTime.ofInstant(value,ZoneOffset.UTC); }
}
