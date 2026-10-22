package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.application.*;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 扫描持久化会话，失败留待下轮重试；多实例由账号锁及 cleaned 标记防重。 */
@Component
public class MediaCleanup {
    private static final Logger LOG=LoggerFactory.getLogger(MediaCleanup.class);
    private final LibraryStore library;private final MediaLibrary mediaLibrary;
    private final MediaStore store; private final MediaService service; private final MediaPolicy policy; private final Clock clock;
    public MediaCleanup(MediaStore store,MediaService service,MediaPolicy policy,Clock clock,LibraryStore library,MediaLibrary mediaLibrary) {
        this.store=store;this.service=service;this.policy=policy;this.clock=clock;this.library=library;this.mediaLibrary=mediaLibrary;
    }
    @Scheduled(initialDelayString="${fafa.media.cleanup-delay-ms:60000}",fixedDelayString="${fafa.media.cleanup-delay-ms:60000}")
    public void scan() {
        if(!policy.cleanupEnabled()) return;
        for(String id:library.pendingDeletes()) {
            try { mediaLibrary.purge(id); }
            catch(RuntimeException e) { LOG.warn("素材清理失败 mediaId={} errorType={}",id,e.getClass().getSimpleName()); }
        }
        for(String id:store.expired(clock.instant())) {
            try { service.cleanup(id); }
            catch(RuntimeException e) { LOG.warn("媒体清理失败 sessionId={} errorType={}",id,e.getClass().getSimpleName()); }
        }
    }
}
