package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.IdentityModels.LegalDocument;
import com.fafa.biz.identity.domain.IdentityRepository;
import com.fafa.biz.identity.domain.IdentityError;
import com.fafa.common.error.FafaException;
import com.fafa.common.error.CommonErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class LegalDocuments {
    private static final Set<String> REQUIRED = Set.of("TERMS", "PRIVACY");
    private final IdentityRepository repository;
    private final Clock clock;
    public LegalDocuments(IdentityRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    public List<LegalDocument> current() {
        Map<String, LegalDocument> current = new LinkedHashMap<>();
        repository.publishedDocuments(LocalDateTime.now(clock))
                .forEach(document -> current.putIfAbsent(document.type(), document));
        return List.copyOf(current.values());
    }
    public void requireAccepted(Map<String, String> accepted) {
        Map<String, String> expected = new HashMap<>();
        current().forEach(document -> {
            if (REQUIRED.contains(document.type())) expected.put(document.type(), document.version());
        });
        if (!expected.keySet().equals(REQUIRED) || !expected.equals(accepted)) {
            throw new FafaException(IdentityError.LEGAL_VERSION_CHANGED);
        }
    }
    public List<LegalDocument> read(String type, String version) {
        if ((type != null && !REQUIRED.contains(type)) || (version != null && type == null)
                || (version != null && (version.isBlank() || version.length() > 64))) {
            throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        }
        if (version != null) {
            var found = repository.publishedVersion(type, version, LocalDateTime.now(clock));
            if (found.isEmpty()) throw new FafaException(CommonErrorCode.NOT_FOUND);
            return found;
        }
        return current().stream().filter(document -> type == null || type.equals(document.type())).toList();
    }
}
