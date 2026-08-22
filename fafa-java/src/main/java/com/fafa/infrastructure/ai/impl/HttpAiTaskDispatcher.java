package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP 同步调用 AI 任务分发实现（开发环境）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.ai.dispatcher", havingValue = "http", matchIfMissing = true)
public class HttpAiTaskDispatcher implements AiTaskDispatcher {

    private final RestTemplate restTemplate;
    private final FaFaProperties properties;

    @Override
    public void dispatchProfilePhotoVectorization(Long petId, MultipartFile frontPhoto,
                                                 MultipartFile sidePhoto, String frontUrl,
                                                 String sideUrl) {
        try {
            String url = properties.getPython().getBaseUrl() + "/api/pets/profile-photos/vectorize";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Internal-Token", properties.getInternalToken());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("pet_id", petId);
            body.add("front_photo", new MultipartInputStreamFileResource(frontPhoto));
            body.add("side_photo", new MultipartInputStreamFileResource(sidePhoto));
            body.add("front_url", frontUrl);
            body.add("side_url", sideUrl);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, requestEntity, String.class);

            log.info("HTTP dispatched profile photo vectorization: petId={}", petId);

        } catch (Exception ex) {
            log.error("Failed to dispatch profile photo vectorization via HTTP: petId={}", petId, ex);
            throw new RuntimeException("AI task dispatch failed", ex);
        }
    }

    private static class MultipartInputStreamFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartInputStreamFileResource(MultipartFile file) throws Exception {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
