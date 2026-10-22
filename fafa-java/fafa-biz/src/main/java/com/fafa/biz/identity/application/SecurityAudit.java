package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.IdentityRepository;
import com.fafa.common.web.RequestIdContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class SecurityAudit {
    private final IdentityRepository repository;
    private final Clock clock;
    public SecurityAudit(IdentityRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    public void record(String userId, String action, String result) {
        repository.audit(UUID.randomUUID().toString(), userId, action, result, LocalDateTime.now(clock),
                RequestIdContext.resolve(MDC.get(RequestIdContext.MDC_KEY)));
    }
}
