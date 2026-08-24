package com.fafa.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.application.dto.content.*;
import com.fafa.application.port.ContentGenerationGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.content.ContentDraft;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.report.ReportPeriod;
import com.fafa.domain.model.report.ReportPeriodType;
import com.fafa.domain.repository.ContentDraftRepository;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDraftApplicationService {
    private final PetRepository petRepository;
    private final ContentDraftRepository draftRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ReportFactAssembler factAssembler;
    private final ContentGenerationGateway generationGateway;
    private final QuotaApplicationService quotaApplicationService;
    private final FaFaProperties properties;
    private final ObjectMapper objectMapper;

    public ContentDraftDTO create(CreateContentDraftCommand command) {
        Pet pet = ownedPet(command.userId(), command.petId());
        validateType(command.contentType());
        List<Long> mediaIds = normalizeMediaIds(command.selectedMediaIds());
        validateMedia(command.userId(), pet.getId(), mediaIds);
        String idempotency = command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                ? "manual-" + java.util.UUID.randomUUID() : command.idempotencyKey().trim();
        var existing = draftRepository.findByIdempotencyKey(command.userId(), idempotency).orElse(null);
        if (existing != null) return toDTO(existing);
        ReportPeriod period = resolvePeriod(command.periodType(), command.anchorDate());
        String factJson = write(factAssembler.assemble(command.userId(), pet.getId(), period));
        ContentDraft draft = draftRepository.save(ContentDraft.create(command.userId(), pet.getId(),
                command.contentType(), defaultTopic(command.topicType()), command.customTopic(),
                period.type().getCode(), period.startDate(), period.endDate(), write(mediaIds), factJson,
                idempotency, defaultTrigger(command.trigger()), LocalDateTime.now()));
        return generate(command.userId(), draft, pet, mediaIds);
    }

    public List<ContentDraftDTO> list(Long userId, Long petId, int page, int size) {
        ownedPet(userId, petId);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return draftRepository.findByUserIdAndPetId(userId, petId, Math.max(0, page - 1) * safeSize, safeSize)
                .stream().map(this::toDTO).toList();
    }

    public ContentDraftDTO get(Long userId, Long id) { return toDTO(ownedDraft(userId, id)); }

    public ContentDraftDTO update(UpdateContentDraftCommand command) {
        ContentDraft draft = ownedDraft(command.userId(), command.draftId());
        validateDraftJson(command.userId(), draft, command.draftJson());
        draft.updateDraft(command.draftJson(), LocalDateTime.now());
        return toDTO(draftRepository.save(draft));
    }

    public void delete(Long userId, Long id) {
        ContentDraft draft = ownedDraft(userId, id);
        draft.delete(); draftRepository.save(draft);
    }

    public ContentDraftDTO regenerate(Long userId, Long id) {
        ContentDraft draft = ownedDraft(userId, id);
        List<Long> mediaIds = readMediaIds(draft.getSelectedMediaJson());
        Pet pet = ownedPet(userId, draft.getPetId());
        draft.markGenerating("regenerate", LocalDateTime.now());
        draft = draftRepository.save(draft);
        return generate(userId, draft, pet, mediaIds);
    }

    private ContentDraftDTO generate(Long userId, ContentDraft draft, Pet pet, List<Long> mediaIds) {
        quotaApplicationService.tryAcquireReportContent(userId);
        try {
            ContentGenerationResponse generated = generationGateway.generate(new ContentGenerationRequest(
                    draft.getId(), pet.getId(), draft.getContentType(), draft.getTopicType(), draft.getCustomTopic(),
                    draft.getFactSnapshotJson(), draft.getSelectedMediaJson(), properties.getReport().getPromptVersion()));
            validateContent(generated.content());
            draft.markCompleted(write(generated.content()), mode(generated.mode()), generated.modelName(),
                    generated.promptVersion(), LocalDateTime.now());
        } catch (BusinessException ex) {
            quotaApplicationService.releaseReportContent(userId);
            draft.markDegraded(write(fallback(pet, draft, mediaIds)), "AI_GENERATION_FAILED",
                    "AI 暂不可用，已生成可编辑草稿", LocalDateTime.now());
            log.warn("内容生成降级: draftId={}, code={}", draft.getId(), ex.getErrorCode());
        } catch (RuntimeException ex) {
            quotaApplicationService.releaseReportContent(userId);
            draft.markDegraded(write(fallback(pet, draft, mediaIds)), "AI_GENERATION_FAILED",
                    "AI 暂不可用，已生成可编辑草稿", LocalDateTime.now());
            log.warn("内容生成异常并降级: draftId={}", draft.getId(), ex);
        }
        return toDTO(draftRepository.save(draft));
    }

    private ContentDraftContentDTO fallback(Pet pet, ContentDraft draft, List<Long> mediaIds) {
        String name = pet.getName() == null ? "宠物" : pet.getName();
        return new ContentDraftContentDTO(name + "的成长记录", "记录一下" + name + "最近的生活点滴，留住每一个值得回看的瞬间。",
                List.of("#宠物日常", "#成长记录"), mediaIds, List.of());
    }

    private void validateContent(ContentDraftContentDTO content) {
        if (content == null || blank(content.title()) || blank(content.body()) || content.title().length() > 100
                || content.body().length() > 5000 || size(content.hashtags()) > 20 || size(content.mediaOrder()) > 12) {
            throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID);
        }
        String text = content.title() + content.body();
        if (List.of("诊断为", "确诊", "处方", "服用剂量", "治疗方案").stream().anyMatch(text::contains)) {
            throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID, "内容包含不允许的医疗结论");
        }
    }

    private void validateMedia(Long userId, Long petId, List<Long> ids) {
        if (ids.isEmpty()) return;
        List<MediaAsset> assets = mediaAssetRepository.findByIds(userId, ids);
        if (assets.size() != ids.size() || assets.stream().anyMatch(item -> !petId.equals(item.getPetId()))) {
            throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID, "所选素材不存在或不属于当前宠物");
        }
    }

    private ReportPeriod resolvePeriod(String type, LocalDate anchor) {
        try { return ReportPeriod.resolve(type == null ? ReportPeriodType.MONTH : ReportPeriodType.fromCode(type), anchor); }
        catch (RuntimeException ex) { throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID, "内容周期无效"); }
    }
    private Pet ownedPet(Long userId, Long id) {
        return petRepository.findById(id).filter(item -> userId.equals(item.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN));
    }
    private ContentDraft ownedDraft(Long userId, Long id) {
        return draftRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_DRAFT_NOT_FOUND));
    }
    private void validateType(String type) {
        if (!List.of("share_note", "xiaohongshu").contains(type)) throw new BusinessException(ErrorCode.CONTENT_TYPE_INVALID);
    }
    private List<Long> normalizeMediaIds(List<Long> ids) {
        if (ids == null) return List.of();
        if (ids.size() > properties.getReport().getMaxMediaCount()) throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID);
        return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
    private List<Long> readMediaIds(String json) {
        try { return objectMapper.readValue(json == null ? "[]" : json, objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)); }
        catch (JsonProcessingException ex) { return List.of(); }
    }
    private void validateDraftJson(Long userId, ContentDraft draft, String json) {
        if (blank(json) || json.length() > 12000) throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID);
        if (List.of("诊断为", "确诊", "处方", "服用剂量", "治疗方案").stream().anyMatch(json::contains)) {
            throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID, "内容包含不允许的医疗结论");
        }
        try {
            JsonNode value = objectMapper.readTree(json);
            JsonNode mediaOrder = value.path("mediaOrder");
            if (mediaOrder.isArray()) {
                List<Long> ids = new java.util.ArrayList<>();
                mediaOrder.forEach(item -> { if (item.canConvertToLong()) ids.add(item.longValue()); });
                validateMedia(userId, draft.getPetId(), ids);
            }
        } catch (JsonProcessingException ex) { throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID); }
    }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID); } }
    private ContentDraftDTO toDTO(ContentDraft item) {
        return new ContentDraftDTO(item.getId(), item.getPetId(), item.getContentType(), item.getTopicType(), item.getCustomTopic(),
                item.getPeriodType(), item.getPeriodStart(), item.getPeriodEnd(), parse(item.getSelectedMediaJson()),
                parse(item.getFactSnapshotJson()), parse(item.getDraftJson()), item.getStatus(), item.getContentMode(),
                item.getGenerationTrigger(), item.getIdempotencyKey(), item.getGenerationVersion(), item.getModelName(),
                item.getPromptVersion(), item.getLastErrorCode(), item.getLastErrorMessage(), item.getCreatedAt(), item.getUpdatedAt());
    }
    private JsonNode parse(String json) { try { return json == null ? objectMapper.createArrayNode() : objectMapper.readTree(json); } catch (JsonProcessingException ex) { return objectMapper.createObjectNode(); } }
    private String defaultTopic(String topic) { return topic == null || topic.isBlank() ? "monthly_summary" : topic; }
    private String defaultTrigger(String trigger) { return trigger == null || trigger.isBlank() ? "manual" : trigger; }
    private String mode(String value) { return value == null || value.isBlank() ? "ai" : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private int size(java.util.Collection<?> value) { return value == null ? 0 : value.size(); }
}
