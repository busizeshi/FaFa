package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.domain.MediaModels.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 会话和已核验素材共享 Biz 数据库事务。 */
@Repository
public class MediaStore implements com.fafa.biz.pet.application.PetResourceCounter {
    private final JdbcTemplate jdbc;
    public MediaStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void insert(Session s) {
        jdbc.update("INSERT INTO upload_session(id,owner_user_id,pet_id,mime,bytes,sha256,expires_at,state,cleaned,upload_mode,multipart_id,reserved_bytes) VALUES(?,?,?,?,?,?,?,?,FALSE,?,?,?)",
            s.id(),s.owner(),s.petId(),s.mime(),s.bytes(),s.sha256(),LocalDateTime.ofInstant(s.expiresAt(),ZoneOffset.UTC),s.state().name(),s.uploadMode().name(),s.multipartId(),s.reservedBytes());
    }
    public Session find(String id) {
        var rows=jdbc.query("SELECT * FROM upload_session WHERE id=?", (r,n)->new Session(
            r.getString("id"),r.getString("owner_user_id"),r.getString("pet_id"),r.getString("mime"),r.getLong("bytes"),
            r.getString("sha256"),r.getObject("expires_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),
            State.valueOf(r.getString("state")),r.getString("asset_id"),r.getBoolean("cleaned"),UploadMode.valueOf(r.getString("upload_mode")),r.getString("multipart_id"),r.getLong("reserved_bytes")),id);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void state(String id, State state, String assetId) {
        jdbc.update("UPDATE upload_session SET state=?,asset_id=? WHERE id=?",state.name(),assetId,id);
    }
    public void ready(Session s,int width,int height) { ready(s,width,height,0,null,null,java.time.Instant.now()); }
    public void ready(Session s,int width,int height,long playbackBytes,Long duration,Integer pages,java.time.Instant now) {

        jdbc.update("INSERT INTO media_asset(id,owner_user_id,pet_id,bytes,mime,sha256,width,height,media_type,captured_at,captured_at_source,charged_bytes,duration_millis,page_count) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            s.id(),s.owner(),s.petId(),s.bytes(),s.mime(),s.sha256(),width,height,type(s.mime()).name(),LocalDateTime.ofInstant(now,ZoneOffset.UTC),"UPLOAD",s.bytes()+playbackBytes,duration,pages);
        state(s.id(),State.READY,s.id());
    }
    public static MediaType type(String mime) {
        return mime.startsWith("image/")?MediaType.IMAGE:mime.startsWith("video/")?MediaType.VIDEO:MediaType.DOCUMENT;
    }
    public AssetView asset(String id) {
        var rows=jdbc.query("SELECT * FROM media_asset WHERE id=?", (r,n)->new AssetView(r.getString("id"),r.getString("pet_id"),
            Long.toString(r.getLong("bytes")),r.getString("mime"),r.getString("sha256"),r.getInt("width"),r.getInt("height"),
            MediaType.valueOf(r.getString("media_type")),State.valueOf(r.getString("state")),r.getLong("version"),
            r.getString("title"),r.getString("description"),tags(id),
            r.getObject("captured_at",LocalDateTime.class).toInstant(ZoneOffset.UTC),r.getString("captured_at_source"),
            r.getObject("duration_millis",Long.class),r.getObject("page_count",Integer.class),Long.toString(r.getLong("charged_bytes"))),id);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public List<String> tags(String id) { return jdbc.query("SELECT tag FROM media_tag WHERE media_id=? ORDER BY tag",(r,n)->r.getString(1),id); }
    public List<String> expired(java.time.Instant now) {
        return jdbc.query("SELECT id FROM upload_session WHERE cleaned=FALSE AND expires_at<=? AND state<>'PROCESSING'             AND NOT EXISTS (SELECT 1 FROM media_processing_job j WHERE j.id=upload_session.id AND j.state<>'CANCELLED' AND j.state<>'COMPLETED' AND j.retain_until>?)             ORDER BY expires_at,id LIMIT 50",
            (r,n)->r.getString(1),LocalDateTime.ofInstant(now,ZoneOffset.UTC),LocalDateTime.ofInstant(now,ZoneOffset.UTC));
    }
    public String avatar(String petId) {
        var ids=jdbc.query("SELECT media_id FROM media_binding WHERE pet_id=?",(r,n)->r.getString(1),petId);
        return ids.isEmpty()?null:ids.getFirst();
    }
    public void avatar(String petId,String mediaId,long version,java.time.Instant now) {
        jdbc.update("DELETE FROM media_binding WHERE pet_id=?",petId);
        jdbc.update("INSERT INTO media_binding(pet_id,media_id) VALUES(?,?)",petId,mediaId);
        int count=jdbc.update("UPDATE pet SET version=version+1,updated_at=? WHERE id=? AND version=?",LocalDateTime.ofInstant(now,ZoneOffset.UTC),petId,version);
        if(count!=1) throw new com.fafa.common.error.FafaException(com.fafa.common.error.CommonErrorCode.CONFLICT);
    }
    @Override
    public java.util.Map<String,Long> countForPet(String petId) {
        return java.util.Map.of(
            "media",jdbc.queryForObject("SELECT COUNT(*) FROM media_asset WHERE pet_id=? AND state<>'DELETED'",Long.class,petId),
            "avatar",jdbc.queryForObject("SELECT COUNT(*) FROM media_binding WHERE pet_id=?",Long.class,petId),
            "pendingUploads",jdbc.queryForObject("SELECT COUNT(*) FROM upload_session WHERE pet_id=? AND cleaned=FALSE AND state IN ('INITIATED','PROCESSING','FAILED','CANCELLED')",Long.class,petId));
    }
    public void cleaned(String id) { jdbc.update("UPDATE upload_session SET cleaned=TRUE WHERE id=?",id); }
}
