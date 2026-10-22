package com.fafa.biz.media.application;

import com.fafa.biz.identity.application.CurrentActor;
import com.fafa.biz.pet.application.PetAccess;
import com.fafa.biz.media.domain.*;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.domain.LibraryModels.*;
import com.fafa.biz.media.infrastructure.MediaStore;
import com.fafa.common.error.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 应用级分片：固定键和精确长度的 POST 分片，服务端按 ETag 条件合并，沿用一次预占。 */
@Service
public class MultipartUploads {
    public static final long PART_BYTES=8L*1024*1024;
    private final CurrentActor actor;private final PetAccess pets;private final MediaStore store;private final ObjectStorage objects;
    private final java.time.Clock clock;
    public MultipartUploads(CurrentActor actor,PetAccess pets,MediaStore store,ObjectStorage objects,java.time.Clock clock) {
        this.actor=actor;this.pets=pets;this.store=store;this.objects=objects;this.clock=clock;
    }
    public MultipartView view(String id) {
        var s=owned(actor.id(),id);requireMultipart(s);
        var parts=new ArrayList<PartView>();
        for(int n=1;n<=count(s);n++) {
            var info=objects.inspect(key(s,n));
            if(info!=null) parts.add(new PartView(n,Long.toString(info.bytes()),info.etag()));
        }
        return new MultipartView(id,s.multipartId(),PART_BYTES,parts);
    }
    @Transactional
    public PartTicket policy(String id,int number) {
        var s=owned(actor.lock(),id);requireMultipart(s);pets.active(s.owner(),s.petId());
        if(s.state()!=State.INITIATED||!s.expiresAt().isAfter(clock.instant())||number>count(s)||number<1) conflict();
        long bytes=length(s,number);
        var p=objects.policy(key(s,number),"application/octet-stream",bytes,s.expiresAt());
        return new PartTicket(number,p.url(),p.fields(),s.expiresAt(),Long.toString(bytes));
    }
    public void assemble(Session s) {
        if(s.uploadMode()!=UploadMode.MULTIPART) return;
        var fragments=new ArrayList<ObjectStorage.Fragment>();
        for(int n=1;n<=count(s);n++) {
            var info=objects.inspect(key(s,n));
            if(info==null||info.bytes()!=length(s,n)) conflict();
            fragments.add(new ObjectStorage.Fragment(key(s,n),info.etag()));
        }
        objects.compose(fragments,s.stagingKey());
    }
    public static void deleteParts(ObjectStorage objects,Session s) {
        if(s.uploadMode()==UploadMode.MULTIPART) for(int n=1;n<=count(s);n++) objects.delete(key(s,n));
    }
    public static int count(Session s) { return (int)((s.bytes()+PART_BYTES-1)/PART_BYTES); }
    private static long length(Session s,int number) { return Math.min(PART_BYTES,s.bytes()-(number-1)*PART_BYTES); }
    public static String key(Session s,int number) { return s.stagingKey()+"/parts/"+number; }
    private Session owned(String owner,String id) {
        var s=store.find(id);if(s==null||!owner.equals(s.owner())) throw new FafaException(CommonErrorCode.NOT_FOUND);return s;
    }
    private void requireMultipart(Session s) { if(s.uploadMode()!=UploadMode.MULTIPART) conflict(); }
    private void conflict() { throw new FafaException(CommonErrorCode.CONFLICT); }
}
