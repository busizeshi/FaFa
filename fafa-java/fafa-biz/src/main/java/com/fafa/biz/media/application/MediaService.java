package com.fafa.biz.media.application;

import com.fafa.biz.identity.application.*;
import com.fafa.biz.pet.application.PetAccess;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.biz.media.infrastructure.MediaStore;
import com.fafa.biz.operation.IdempotentWrites;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.common.error.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 单图上传用例。账号锁使上传、取消、宠物状态转换和结算串行化。 */
@Service
public class MediaService {
    private final CurrentActor actor;
    private final AccountAdministration users;
    private final PetAccess pets;
    private final StorageQuota quota;
    private final IdempotentWrites writes;
    private final MediaStore store;
    private final ObjectStorage objects;
    private final ImageVerifier verifier;
    private final MediaPolicy policy;
    private final Clock clock;
    private final ProcessingPolicy processing;
    private final MediaJobService jobs;
    private final MultipartUploads multipart;
    public MediaService(CurrentActor actor,AccountAdministration users,PetAccess pets,StorageQuota quota,
            IdempotentWrites writes,MediaStore store,ObjectStorage objects,ImageVerifier verifier,MediaPolicy policy,Clock clock,ProcessingPolicy processing,MediaJobService jobs,MultipartUploads multipart) {
        this.actor=actor;this.users=users;this.pets=pets;this.quota=quota;this.writes=writes;
        this.store=store;this.objects=objects;this.verifier=verifier;this.policy=policy;this.clock=clock;this.processing=processing;this.jobs=jobs;this.multipart=multipart;
    }
    @Transactional
    public UploadView create(String key,CreateUpload request) {
        String owner=actor.lock();
        pets.active(owner,request.petId());
        long bytes=Long.parseLong(request.bytes());
        long max=request.mime().startsWith("image/")?policy.maxImageBytes():request.mime().equals("application/pdf")?
            processing.maxDocumentBytes():processing.maxVideoBytes();
        if(request.mode()==UploadMode.MULTIPART&&!request.mime().startsWith("video/")) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        if(bytes>max) throw new FafaException(CommonErrorCode.REQUEST_TOO_LARGE);
        // 只缓存无签名的会话摘要；临时上传凭证通过单独接口按剩余期限签发。
        return writes.execute(owner,"CREATE_UPLOAD",key,request,UploadView.class,()->{
            String id=UUID.randomUUID().toString();
            var session=new Session(id,owner,request.petId(),request.mime(),bytes,request.sha256(),
                clock.instant().plusSeconds(policy.uploadSeconds()),State.INITIATED,null,false,request.mode(),
                request.mode()==UploadMode.MULTIPART?id:null,bytes+(request.mime().startsWith("video/")?processing.maxPlaybackBytes():0));
            quota.reserve(owner,session.reservationId(),session.reservedBytes());
            store.insert(session);
            return session.view();
        });
    }
    public UploadView get(String id) { return owned(actor.id(),id).view(); }
    @Transactional
    public UploadPolicy uploadPolicy(String id) {
        var s=owned(actor.lock(),id); requireOpen(s);
        if(s.uploadMode()!=UploadMode.SINGLE) throw new FafaException(CommonErrorCode.CONFLICT);
        pets.active(s.owner(),s.petId());
        var signed=objects.policy(s.stagingKey(),s.mime(),s.bytes(),s.expiresAt());
        return new UploadPolicy(id,signed.url(),signed.fields(),s.expiresAt());
    }
    @Transactional(noRollbackFor=InvalidImage.class)
    public UploadView complete(String id) {
        var s=owned(actor.lock(),id);
        if(s.state()==State.READY||s.state()==State.PROCESSING) return s.view();
        requireOpen(s); pets.active(s.owner(),s.petId());
        multipart.assemble(s);
        if(!s.mime().startsWith("image/")) return jobs.enqueue(s);
        byte[] raw=objects.read(s.stagingKey(),s.bytes());
        ImageVerifier.Verified verified;
        try { verified=verifier.verify(raw,s.mime(),s.sha256()); }
        catch(FafaException e) {
            store.state(id,State.FAILED,null);
            throw new InvalidImage();
        }
        // 正式对象从本次已验证字节写入，不引用可在有效期内被重传覆盖的隔离对象。
        objects.put(s.originalKey(),raw,s.mime());
        objects.put(s.previewKey(),verified.preview(),"image/png");
        quota.settle(s.owner(),s.reservationId(),raw.length);
        store.ready(s,verified.width(),verified.height(),0,null,null,clock.instant());
        return store.find(id).view();
    }
    @Transactional
    public UploadView cancel(String id) {
        var s=owned(actor.lock(),id);
        if(s.state()==State.READY||s.state()==State.DELETION_PENDING||s.state()==State.DELETED) throw new FafaException(CommonErrorCode.CONFLICT);
        if(s.state()==State.INITIATED||s.state()==State.PROCESSING||s.state()==State.FAILED) { jobs.cancel(id);store.state(id,State.CANCELLED,null); }
        return store.find(id).view();
    }
    public AssetView asset(String id) {
        ready(actor.id(),id);
        return store.asset(id);
    }
    public AccessTicket access(String id,Variant variant) {
        var s=ready(actor.id(),id);
        if(variant==Variant.PLAYBACK&&!s.mime().startsWith("video/")) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        if(variant==Variant.DOCUMENT&&!"application/pdf".equals(s.mime())) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        String url=variant==Variant.DOCUMENT?objects.downloadAttachment(s.originalKey(),policy.accessSeconds()):
            objects.download(variant==Variant.PLAYBACK?s.playbackKey():s.previewKey(),policy.accessSeconds());
        return new AccessTicket(url,clock.instant().plusSeconds(policy.accessSeconds()));
    }
    public AvatarView avatar(String petId) {
        var pet=pets.owned(actor.id(),petId);
        if(pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ACTIVE &&
           pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ARCHIVED) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return new AvatarView(petId,store.avatar(petId),pet.version());
    }
    @Transactional
    public AvatarView bindAvatar(String petId,String key,AvatarChange change) {
        String owner=actor.lock();
        return writes.execute(owner,"BIND_AVATAR:"+petId,key,change,AvatarView.class,()->{
            pets.versioned(owner,petId,change.version()); pets.active(owner,petId);
            var s=ready(owner,change.mediaId());
            if(!s.mime().startsWith("image/")||!s.petId().equals(petId)) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
            store.avatar(petId,s.id(),change.version(),clock.instant());
            return new AvatarView(petId,s.id(),change.version()+1);
        });
    }
    @Transactional
    public void cleanup(String id) {
        var before=store.find(id);
        if(before==null) return;
        users.lock(before.owner());
        var s=store.find(id);
        if(s.cleaned() || s.expiresAt().isAfter(clock.instant())||s.state()==State.PROCESSING||
            s.state()==State.DELETION_PENDING||s.state()==State.DELETED||!jobs.canCleanup(id)) return;
        MultipartUploads.deleteParts(objects,s);
        objects.delete(s.stagingKey());
        if(s.state()!=State.READY) {
            objects.delete(s.originalKey());objects.delete(s.previewKey());objects.delete(s.playbackKey());
            quota.release(s.owner(),s.reservationId());
            if(s.state()==State.INITIATED) store.state(id,State.EXPIRED,null);
        }
        store.cleaned(id);
    }
    private Session owned(String owner,String id) {
        var s=store.find(id);
        if(s==null || !s.owner().equals(owner)) throw new FafaException(CommonErrorCode.NOT_FOUND);
        return s;
    }
    private Session ready(String owner,String id) {
        var s=owned(owner,id);
        var pet=pets.owned(owner,s.petId());
        if(pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ACTIVE &&
           pet.status()!=com.fafa.biz.pet.domain.PetModels.Status.ARCHIVED) throw new FafaException(CommonErrorCode.NOT_FOUND);
        if(s.state()!=State.READY) throw new FafaException(CommonErrorCode.CONFLICT);
        return s;
    }
    private void requireOpen(Session s) {
        if(s.state()!=State.INITIATED || !s.expiresAt().isAfter(clock.instant())) throw new FafaException(CommonErrorCode.CONFLICT);
    }
    /** 无效图片仍须提交失败状态，预占由凭证到期后的清理事务释放。 */
    public static class InvalidImage extends FafaException {
        public InvalidImage() { super(CommonErrorCode.INVALID_ARGUMENT,"图片验证失败，请重新创建上传会话"); }
    }
}
