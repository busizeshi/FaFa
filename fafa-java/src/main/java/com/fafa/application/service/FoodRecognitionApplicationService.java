package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.food.FoodCardDTO;
import com.fafa.application.dto.food.FoodRecognitionDTO;
import com.fafa.application.port.FoodRecognitionGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.food.FoodRecognitionStatus;
import com.fafa.domain.model.food.FoodRecognitionTask;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.model.media.MediaType;
import com.fafa.domain.repository.FoodRecognitionTaskRepository;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.interfaces.dto.food.ConfirmFoodRecognitionRequest;
import com.fafa.infrastructure.config.FaFaProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 食品包装识别任务编排服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecognitionApplicationService {

    private final FoodRecognitionTaskRepository taskRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final FoodRecognitionGateway gateway;
    private final FoodCardApplicationService foodCardApplicationService;
    private final PetApplicationService petApplicationService;
    private final QuotaApplicationService quotaApplicationService;
    private final ObjectMapper objectMapper;
    private final FaFaProperties properties;

    public FoodRecognitionDTO create(Long petId, Long mediaId, String requestKey) {
        Long userId = currentUserId();
        petApplicationService.checkPetOwnership(petId);
        MediaAsset media = mediaAssetRepository.findById(mediaId)
                .filter(item -> item.getUserId().equals(userId)
                        && item.getMediaType() == MediaType.PHOTO && !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_MEDIA_INVALID));
        String idempotencyKey = normalizeKey(requestKey);
        var existing = taskRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }

        quotaApplicationService.tryAcquireFoodRecognition(userId);
        FoodRecognitionTask task = FoodRecognitionTask.create(userId, petId, mediaId, idempotencyKey,
                YearMonth.now().toString());
        try {
            task = taskRepository.save(task);
            task.markProcessing(properties.getFood().getRecognition().getPromptVersion(), LocalDateTime.now());
            task = taskRepository.save(task);
            var result = gateway.recognize(new FoodRecognitionGateway.FoodRecognitionRequest(
                    task.getId(), userId, petId, mediaId, media.getFileUrl(), task.getPromptVersion(), traceId()));
            String draftJson = result.draft().toString();
            task.complete(result.model(), result.promptVersion(), draftJson, result.rawResponse().toString(),
                    LocalDateTime.now());
            task = taskRepository.save(task);
            log.info("食品包装识别完成: userId={}, petId={}, taskId={}", userId, petId, task.getId());
            return toDTO(task, result.warnings());
        } catch (RuntimeException ex) {
            quotaApplicationService.releaseFoodRecognition(userId);
            if (task.getId() != null) {
                task.fail("FOOD_RECOGNITION_FAILED", safeMessage(ex), LocalDateTime.now());
                taskRepository.save(task);
            }
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    public FoodRecognitionDTO get(Long id) {
        return toDTO(expireIfNeeded(findOwned(id)));
    }

    @Transactional
    public FoodCardDTO confirm(Long id, ConfirmFoodRecognitionRequest request) {
        FoodRecognitionTask task = expireIfNeeded(findOwned(id));
        if (task.getStatus() == FoodRecognitionStatus.CONFIRMED) {
            return foodCardApplicationService.get(task.getFoodCardId());
        }
        if (task.getStatus() == FoodRecognitionStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.FOOD_RECOGNITION_EXPIRED);
        }
        if (task.getStatus() != FoodRecognitionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.FOOD_RECOGNITION_NOT_READY);
        }
        var existingFood = foodCardApplicationService.findBySourceRecognitionTaskId(task.getId());
        if (existingFood.isPresent()) {
            task.confirm(existingFood.get().id());
            taskRepository.save(task);
            return existingFood.get();
        }
        FoodCardDTO foodCard = foodCardApplicationService.create(
                request.toCommand(task.getPetId(), task.getId(), task.getMediaId()));
        task.confirm(foodCard.id());
        taskRepository.save(task);
        return foodCard;
    }

    public void reject(Long id, String reason) {
        FoodRecognitionTask task = findOwned(id);
        if (task.getStatus() == FoodRecognitionStatus.REJECTED) {
            return;
        }
        try {
            task.reject(reason, LocalDateTime.now());
            taskRepository.save(task);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.FOOD_RECOGNITION_NOT_READY, ex.getMessage());
        }
    }

    private FoodRecognitionTask findOwned(Long id) {
        return taskRepository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
    }

    private FoodRecognitionTask expireIfNeeded(FoodRecognitionTask task) {
        if (task.getStatus() == FoodRecognitionStatus.COMPLETED
                && task.getCompletedAt() != null
                && task.getCompletedAt().plusDays(properties.getFood().getRecognition().getDraftRetentionDays())
                .isBefore(LocalDateTime.now())) {
            task.expire(LocalDateTime.now());
            return taskRepository.save(task);
        }
        return task;
    }

    private FoodRecognitionDTO toDTO(FoodRecognitionTask task) {
        return toDTO(task, List.of());
    }

    private FoodRecognitionDTO toDTO(FoodRecognitionTask task, List<String> warnings) {
        if (warnings.isEmpty()) {
            warnings = readWarnings(task.getRawResponseJson());
        }
        return new FoodRecognitionDTO(task.getId(), task.getPetId(), task.getMediaId(), task.getStatus().getCode(),
                parseDraft(task.getDraftJson()), warnings, task.getModelName(), task.getPromptVersion(),
                task.getFoodCardId(), task.getErrorCode(), task.getErrorMessage(), task.getCreatedAt(),
                task.getCompletedAt());
    }

    private List<String> readWarnings(String rawResponseJson) {
        if (rawResponseJson == null || rawResponseJson.isBlank()) return List.of();
        try {
            Map<String, Object> raw = objectMapper.readValue(rawResponseJson, new TypeReference<>() { });
            Object value = raw.get("warnings");
            if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        } catch (Exception ex) {
            log.warn("食品识别原始响应提示解析失败: error={}", ex.getMessage());
        }
        return List.of();
    }

    private Map<String, Object> parseDraft(String draftJson) {
        if (draftJson == null || draftJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(draftJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("食品识别草稿 JSON 无效: error={}", ex.getMessage());
            return Map.of();
        }
    }

    private String normalizeKey(String requestKey) {
        return requestKey == null || requestKey.isBlank()
                ? "food-recognition-" + UUID.randomUUID()
                : requestKey.trim();
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? "食品识别失败" : message.substring(0, Math.min(500, message.length()));
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private String traceId() {
        String trace = org.slf4j.MDC.get("traceId");
        return trace == null || trace.isBlank() ? "no-trace" : trace;
    }
}
