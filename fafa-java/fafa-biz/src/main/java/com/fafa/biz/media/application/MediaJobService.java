package com.fafa.biz.media.application;

import com.fafa.biz.identity.application.*;
import com.fafa.biz.pet.application.PetAccess;
import com.fafa.biz.media.domain.*;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.domain.LibraryModels.JobView;
import com.fafa.biz.media.infrastructure.*;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.common.error.*;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 作业状态事务与租约栅栏；耗时解码在事务之外，正式对象写入只允许当前租约持有者。 */
@Service
public class MediaJobService {
    public record Lease(Session session,String token) { }
    private final CurrentActor actor;private final AccountAdministration users;private final PetAccess pets;
    private final MediaStore media;private final MediaJobStore jobs;private final ObjectStorage storage;
    private final StorageQuota quota;private final ProcessingPolicy policy;private final Clock clock;
    public MediaJobService(CurrentActor actor,AccountAdministration users,PetAccess pets,MediaStore media,MediaJobStore jobs,
            ObjectStorage storage,StorageQuota quota,ProcessingPolicy policy,Clock clock) {
        this.actor=actor;this.users=users;this.pets=pets;this.media=media;this.jobs=jobs;this.storage=storage;this.quota=quota;this.policy=policy;this.clock=clock;
    }
    @Transactional
    public UploadView enqueue(Session s) {
        var info=storage.inspect(s.stagingKey());
        if(info==null||info.bytes()!=s.bytes()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"上传对象大小不符");
        storage.copy(s.stagingKey(),s.originalKey());
        jobs.create(s.id(),s.owner(),clock.instant().plusSeconds(policy.retentionSeconds()));
        jobs.queue(s.id(),clock.instant(),clock.instant());media.state(s.id(),State.PROCESSING,null);
        return media.find(s.id()).view();
    }
    @Transactional
    public Lease claim(String eventId,String id) {
        var initial=jobs.find(id);if(initial==null) return null;
        users.lock(initial.owner());var job=jobs.find(id);
        if(!jobs.matches(eventId,id)||jobs.seen(eventId)) return null;
        jobs.receipt(eventId,clock.instant());
        if(!"QUEUED".equals(job.state())) return null;
        if(!job.retainUntil().isAfter(clock.instant())) { jobs.state(id,"FAILED","RETENTION_EXPIRED");media.state(id,State.FAILED,null);return null; }
        String token=UUID.randomUUID().toString();
        jobs.claim(id,token,clock.instant().plusSeconds(policy.leaseSeconds()));
        return new Lease(media.find(id),token);
    }
    @Transactional
    public boolean finish(Lease lease,FileProcessor.Processed result) {
        Session s=lease.session();users.lock(s.owner());var job=jobs.find(s.id());
        if(!valid(job,lease)) return false;
        var user=users.lock(s.owner());
        if(!"ACTIVE".equals(user.status())) throw new FafaException(CommonErrorCode.FORBIDDEN);
        pets.active(s.owner(),s.petId());
        storage.putFile(s.previewKey(),result.preview(),"image/png");
        long playback=0;
        if(result.playback()!=null) {
            try { playback=java.nio.file.Files.size(result.playback()); }
            catch(java.io.IOException e) { throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE); }
            if(playback>policy.maxPlaybackBytes()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
            storage.putFile(s.playbackKey(),result.playback(),"video/mp4");
        }
        quota.settle(s.owner(),s.reservationId(),s.bytes()+playback);
        media.ready(s,result.width(),result.height(),playback,result.durationMillis(),result.pageCount(),clock.instant());
        jobs.state(s.id(),"COMPLETED",null);return true;
    }
    @Transactional
    public void fail(Lease lease,boolean retryable,String code) {
        var s=lease.session();users.lock(s.owner());var job=jobs.find(s.id());
        if(!valid(job,lease)) return;
        if(retryable&&job.attempts()<policy.maxAttempts()&&job.retainUntil().isAfter(clock.instant())) {
            jobs.queue(s.id(),clock.instant().plusSeconds(10L*job.attempts()),clock.instant());
        } else { jobs.state(s.id(),"FAILED",code);media.state(s.id(),State.FAILED,null); }
    }
    public JobView view(String id) { return owned(actor.id(),id).view(); }
    @Transactional
    public JobView retry(String id) {
        String owner=actor.lock();var job=owned(owner,id);var s=media.find(id);
        if("QUEUED".equals(job.state())||"RUNNING".equals(job.state())) return job.view();
        if(!"FAILED".equals(job.state())||s.cleaned()||!job.retainUntil().isAfter(clock.instant())||job.attempts()>=policy.maxAttempts())
            throw new FafaException(CommonErrorCode.CONFLICT,"任务状态或重试次数不允许");
        pets.active(owner,s.petId());
        jobs.queue(id,clock.instant(),clock.instant());media.state(id,State.PROCESSING,null);return jobs.find(id).view();
    }
    @Transactional
    public void cancel(String id) { if(jobs.find(id)!=null) jobs.state(id,"CANCELLED",null); }
    public boolean canCleanup(String id) {
        var job=jobs.find(id);
        return job==null||"COMPLETED".equals(job.state())||"CANCELLED".equals(job.state())||
            ("FAILED".equals(job.state())&&!job.retainUntil().isAfter(clock.instant()));
    }
    @Transactional
    public void recover(String id) {
        var before=jobs.find(id);if(before==null) return;users.lock(before.owner());var job=jobs.find(id);
        if("RUNNING".equals(job.state())&&job.leaseUntil()!=null&&!job.leaseUntil().isAfter(clock.instant())) {
            if(job.attempts()<policy.maxAttempts()&&job.retainUntil().isAfter(clock.instant())) jobs.queue(id,clock.instant(),clock.instant());
            else { jobs.state(id,"FAILED","LEASE_EXPIRED");media.state(id,State.FAILED,null); }
        } else if("QUEUED".equals(job.state())&&!job.retainUntil().isAfter(clock.instant())) {
            jobs.state(id,"FAILED","RETENTION_EXPIRED");media.state(id,State.FAILED,null);
        } else if("QUEUED".equals(job.state())&&jobs.resendNeeded(id,clock.instant())) jobs.queue(id,clock.instant(),clock.instant());
    }
    private boolean valid(MediaJobStore.Job job,Lease lease) {
        return job!=null&&"RUNNING".equals(job.state())&&lease.token().equals(job.token())&&job.leaseUntil().isAfter(clock.instant());
    }
    private MediaJobStore.Job owned(String owner,String id) {
        var job=jobs.find(id);if(job==null||!job.owner().equals(owner)) throw new FafaException(CommonErrorCode.NOT_FOUND);return job;
    }
}
