package com.fafa.biz.media.application;

import com.fafa.biz.identity.application.*;
import com.fafa.biz.pet.application.PetAccess;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.domain.LibraryModels.*;
import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.biz.media.infrastructure.*;
import com.fafa.biz.operation.*;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.common.api.CursorPage;
import com.fafa.common.error.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 素材与相册整理；所有写入与头像/宠物生命周期共用账号锁。 */
@Service
public class MediaLibrary {
    private final CurrentActor actor; private final AccountAdministration users; private final PetAccess pets;
    private final LibraryStore library; private final MediaStore store; private final ObjectStorage objects;
    private final IdempotentWrites writes; private final Paging paging; private final StorageQuota quota; private final Clock clock;
    public MediaLibrary(CurrentActor actor,AccountAdministration users,PetAccess pets,LibraryStore library,MediaStore store,
            ObjectStorage objects,IdempotentWrites writes,Paging paging,StorageQuota quota,Clock clock) {
        this.actor=actor;this.users=users;this.pets=pets;this.library=library;this.store=store;
        this.objects=objects;this.writes=writes;this.paging=paging;this.quota=quota;this.clock=clock;
    }
    public CursorPage<AssetView> list(LibraryQuery query) {
        String owner=actor.id();
        if(query.q()!=null&&query.q().length()>80 || query.tag()!=null&&query.tag().length()>30 ||
           query.from()!=null&&query.to()!=null&&!query.from().isBefore(query.to())) invalid();
        if(query.petId()!=null) readable(owner,query.petId());
        if(query.albumId()!=null) library.album(owner,query.albumId());
        int limit=paging.limit(query.limit());
        var rows=library.list(owner,query,limit+1);
        return new CursorPage<>(rows.stream().limit(limit).toList(),rows.size()>limit?rows.get(limit-1).id():null);
    }
    @Transactional
    public AssetView update(String id,MetadataChange body) {
        var a=owned(actor.lock(),id);pets.active(actor.id(),a.petId());version(a.version(),body.version());
        if(a.state()!=State.READY) conflict();
        if(body.petId()!=null&&!body.petId().equals(a.petId())) {
            pets.active(actor.id(),body.petId());
            var impact=library.impact(a);if(impact.avatarReferences()+impact.businessReferences()>0) conflict();
        }
        if(body.capturedAt()!=null&&body.capturedAt().isAfter(clock.instant())) invalid();
        if(body.tags().stream().anyMatch(tag->tag.strip().isEmpty())) invalid();
        library.metadata(id,body);return store.asset(id);
    }
    public CursorPage<AlbumView> albums(String cursor,int requested) {
        int limit=paging.limit(requested);var rows=library.albums(actor.id(),cursor,limit+1);
        return new CursorPage<>(rows.stream().limit(limit).toList(),rows.size()>limit?rows.get(limit-1).id():null);
    }
    public AlbumView album(String id) { return library.album(actor.id(),id); }
    @Transactional
    public AlbumView createAlbum(String key,AlbumCreate body) {
        String owner=actor.lock();
        return writes.execute(owner,"CREATE_ALBUM",key,body,AlbumView.class,()->{
            String id=UUID.randomUUID().toString();library.createAlbum(owner,id,body);return library.album(owner,id);
        });
    }
    @Transactional
    public AlbumView updateAlbum(String id,AlbumChange body) {
        String owner=actor.lock();version(library.album(owner,id).version(),body.version());
        library.updateAlbum(id,body);return library.album(owner,id);
    }
    @Transactional
    public void deleteAlbum(String id,long expected) {
        String owner=actor.lock();version(library.album(owner,id).version(),expected);library.deleteAlbum(id);
    }
    @Transactional
    public AlbumView item(String albumId,String mediaId,boolean add) {
        String owner=actor.lock();library.album(owner,albumId);
        if(add) { var a=owned(owner,mediaId);pets.active(owner,a.petId());if(a.state()!=State.READY) conflict();if(a.mediaType()==MediaType.DOCUMENT) invalid(); }
        library.membership(albumId,mediaId,add);return library.album(owner,albumId);
    }
    public DeletionImpact impact(String id) { return library.impact(owned(actor.id(),id)); }
    @Transactional
    public DeletionReceipt delete(String id,DeleteMedia body) {
        String owner=actor.lock();var a=owned(owner,id);
        if(a.state()==State.DELETION_PENDING||a.state()==State.DELETED) return receipt(a);
        pets.active(owner,a.petId());version(a.version(),body.version());
        var impact=library.impact(a);
        if(!body.acknowledgedReferences() && impact.avatarReferences()+impact.albumReferences()+impact.businessReferences()>0) conflict();
        library.deleting(a,clock.instant());return receipt(store.asset(id));
    }
    @Transactional
    public void purge(String id) {
        var session=store.find(id);if(session==null) return;
        users.lock(session.owner());var a=store.asset(id);
        if(a==null||a.state()!=State.DELETION_PENDING||session.expiresAt().isAfter(clock.instant())) return;
        var impact=library.impact(a);
        if(impact.avatarReferences()+impact.albumReferences()+impact.businessReferences()!=0) conflict();
        objects.delete(session.originalKey());objects.delete(session.previewKey());objects.delete(session.playbackKey());
        MultipartUploads.deleteParts(objects,session);
        objects.delete(session.stagingKey());
        quota.freeUsed(session.owner(),session.reservationId());
        library.deleted(id);store.cleaned(id);
    }
    public AssetView owned(String owner,String id) {
        var session=store.find(id);
        if(session==null||!session.owner().equals(owner)) throw new FafaException(CommonErrorCode.NOT_FOUND);
        var asset=store.asset(id);if(asset==null) conflict();
        readable(owner,asset.petId());return asset;
    }
    private void readable(String owner,String petId) {
        var pet=pets.owned(owner,petId);
        if(pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ACTIVE&&pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ARCHIVED)
            throw new FafaException(CommonErrorCode.NOT_FOUND);
    }
    private DeletionReceipt receipt(AssetView a) { return new DeletionReceipt(a.id(),a.state(),a.state()==State.DELETED?"0":a.chargedBytes()); }
    private void version(long actual,long expected) { if(actual!=expected) throw new FafaException(BusinessError.VERSION_CONFLICT); }
    private void invalid() { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT); }
    private void conflict() { throw new FafaException(CommonErrorCode.CONFLICT); }
}
