package com.fafa.biz.media.application;

import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.common.error.*;
import java.nio.file.*;
import org.springframework.stereotype.Component;

/** 仅在数据库领取租约后处理文件；不依赖内存队列维持作业状态。 */
@Component
public class MediaWorker {
    private final MediaJobService jobs;private final FileProcessor processor;private final ObjectStorage objects;
    public MediaWorker(MediaJobService jobs,FileProcessor processor,ObjectStorage objects) { this.jobs=jobs;this.processor=processor;this.objects=objects; }
    public void run(String eventId,String jobId) {
        var lease=jobs.claim(eventId,jobId);if(lease==null) return;
        Path dir=null;
        try {
            dir=Files.createTempDirectory("fafa-media-");
            Path source=dir.resolve("source");
            objects.fetch(lease.session().originalKey(),lease.session().bytes(),source);
            jobs.finish(lease,processor.process(lease.session(),source,dir));
        } catch(FafaException e) {
            jobs.fail(lease,e.errorCode()==CommonErrorCode.DEPENDENCY_UNAVAILABLE,e.errorCode().code());
        } catch(Exception e) { jobs.fail(lease,true,"PROCESSOR_UNAVAILABLE"); }
        finally {
            if(dir!=null) try(var paths=Files.walk(dir)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try { Files.deleteIfExists(path); } catch(java.io.IOException ignored) { }});
            } catch(java.io.IOException ignored) { }
        }
    }
}
