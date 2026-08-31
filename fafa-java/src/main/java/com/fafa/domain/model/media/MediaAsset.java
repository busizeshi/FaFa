package com.fafa.domain.model.media;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 媒体素材（聚合根）
 *
 * 一行 = 一个照片或视频素材 + 向量化状态 + 宠物归属标签。
 *
 * 业务规则（见 phase4-implementation-plan.md 4.2）：
 * 1. photo/video 落库 status=pending（等 embedding 回调）
 * 2. 用户提交的 tags 去重去空、≤10 个；自动追加的宠物名称不受总数上限影响
 * 3. 上传指定 petId → recognitionConfirmed=true、recognitionScore=null（用户明确归属）
 * 4. 多宠物识别结果以宠物名称 tag 为准；单个匹配结果可兼容性回填 petId
 * 5. 已终态（completed/failed/skipped）时重复回调幂等跳过
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public class MediaAsset {

    /** 用户手工提交的素材标签数量上限；自动宠物标签不受此限制 */
    public static final int MAX_TAG_COUNT = 10;
    /** 单个标签最大长度（素材 tags 快照口径） */
    public static final int MAX_TAG_LENGTH = 32;
    /** 历史 AI 描述字段最大长度（当前 embedding 链路不生成描述） */
    public static final int MAX_AI_DESCRIPTION_LENGTH = 1000;

    private Long id;
    private final Long userId;
    private final Long operatorId;
    /** 单宠物兼容字段；多宠物归属以 tags 中的宠物名称为准 */
    private Long petId;
    private final MediaType mediaType;
    private final String fileUrl;
    private final String fileName;
    /** 文件大小（字节，存储配额计量） */
    private final long fileSize;
    private final String mimeType;
    /** 标签名快照 */
    private List<String> tags;
    private MediaAnalysisStatus status;
    private String aiDescription;
    private List<String> aiTags;
    /** 宠物识别相似度（cosine 0~1；用户手动指定归属时为 null） */
    private BigDecimal recognitionScore;
    /** 归属是否已确认 */
    private boolean recognitionConfirmed;
    /** 拍摄/发生时间（可编辑） */
    private LocalDateTime takenAt;
    private String errorMessage;
    private boolean deleted;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MediaAsset(Long id, Long userId, Long operatorId, Long petId, MediaType mediaType, String fileUrl,
                       String fileName, long fileSize, String mimeType, List<String> tags,
                       MediaAnalysisStatus status, String aiDescription, List<String> aiTags,
                       BigDecimal recognitionScore, boolean recognitionConfirmed,
                       LocalDateTime takenAt, String errorMessage, boolean deleted,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.operatorId = operatorId;
        this.petId = petId;
        this.mediaType = mediaType;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.tags = tags;
        this.status = status;
        this.aiDescription = aiDescription;
        this.aiTags = aiTags;
        this.recognitionScore = recognitionScore;
        this.recognitionConfirmed = recognitionConfirmed;
        this.takenAt = takenAt;
        this.errorMessage = errorMessage;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建素材
     *
     * @param petId   上传时指定的归属宠物，可空（等 AI 识别）
     * @param takenAt 拍摄/发生时间，非空（缺省由应用层填上传时刻）
     */
    public static MediaAsset create(Long userId, MediaType mediaType, String fileUrl, String fileName,
                                    long fileSize, String mimeType, List<String> tags,
                                    Long petId, LocalDateTime takenAt, LocalDateTime now) {
        return create(userId, mediaType, fileUrl, fileName, fileSize, mimeType, tags, petId, takenAt, userId, now);
    }

    public static MediaAsset create(Long userId, MediaType mediaType, String fileUrl, String fileName,
                                    long fileSize, String mimeType, List<String> tags,
                                    Long petId, LocalDateTime takenAt, Long operatorId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(mediaType, "mediaType 不能为空");
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("文件地址不能为空");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必须大于 0");
        }
        Objects.requireNonNull(takenAt, "takenAt 不能为空");
        List<String> normalizedTags = normalizeTags(tags);

        // 上传指定归属 → 直接确认；识别相似度无意义置 null
        boolean confirmed = petId != null;
        return new MediaAsset(null, userId, operatorId, petId, mediaType, fileUrl, fileName, fileSize, mimeType,
                normalizedTags, MediaAnalysisStatus.PENDING, null, null, null, confirmed,
                takenAt, null, false, now, now);
    }

    /**
     * 仓储重建
     */
    public static MediaAsset restore(Long id, Long userId, Long petId, MediaType mediaType, String fileUrl,
                                     String fileName, long fileSize, String mimeType, List<String> tags,
                                     MediaAnalysisStatus status, String aiDescription, List<String> aiTags,
                                     BigDecimal recognitionScore, boolean recognitionConfirmed,
                                     LocalDateTime takenAt, String errorMessage, boolean deleted,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return restore(id, userId, petId, mediaType, fileUrl, fileName, fileSize, mimeType, tags, status,
                aiDescription, aiTags, recognitionScore, recognitionConfirmed, takenAt, errorMessage, deleted,
                null, createdAt, updatedAt);
    }

    public static MediaAsset restore(Long id, Long userId, Long petId, MediaType mediaType, String fileUrl,
                                     String fileName, long fileSize, String mimeType, List<String> tags,
                                     MediaAnalysisStatus status, String aiDescription, List<String> aiTags,
                                     BigDecimal recognitionScore, boolean recognitionConfirmed,
                                     LocalDateTime takenAt, String errorMessage, boolean deleted, Long operatorId,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new MediaAsset(id, userId, operatorId, petId, mediaType, fileUrl, fileName, fileSize, mimeType,
                tags, status, aiDescription, aiTags, recognitionScore, recognitionConfirmed,
                takenAt, errorMessage, deleted, createdAt, updatedAt);
    }

    /**
     * 历史视觉分析回调兼容入口；新的 embedding 回调使用 markVectorizationCompleted。
     *
     * 幂等：已终态直接跳过。识别归属仅在「petId 为空且未确认」时回填（confirmed 保持 false 待确认）；
     * 用户已指定归属则只更新相似度，不覆盖归属。
     */
    @Deprecated
    public void markAnalysisCompleted(String description, List<String> aiTags,
                                      Long recognizedPetId, BigDecimal score) {
        if (status.isTerminal()) {
            return;
        }
        if (description != null && description.length() > MAX_AI_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_AI_DESCRIPTION_LENGTH);
        }
        this.status = MediaAnalysisStatus.COMPLETED;
        this.aiDescription = description;
        this.aiTags = normalizeTags(aiTags);
        this.recognitionScore = score;
        this.errorMessage = null;
        if (petId == null && !recognitionConfirmed && recognizedPetId != null) {
            this.petId = recognizedPetId;
        }
    }

    /**
     * embedding 向量化完成并返回宠物匹配结果。
     *
     * <p>多个匹配结果不能写入现有单值 petId 字段，因此由应用层把所有匹配宠物名称追加到 tags；
     * 只有恰好一个结果时才回填 petId，兼容旧的详情和权限逻辑。方法返回是否实际发生状态变更，
     * 用于防止重复回调重复增加标签使用次数。</p>
     */
    public boolean markVectorizationCompleted(List<Long> recognizedPetIds,
                                              BigDecimal score,
                                              LocalDateTime now) {
        if (status.isTerminal()) {
            return false;
        }
        List<Long> normalizedIds = recognizedPetIds == null
                ? List.of()
                : recognizedPetIds.stream().filter(Objects::nonNull).distinct().toList();
        if (petId == null && !recognitionConfirmed && normalizedIds.size() == 1) {
            this.petId = normalizedIds.getFirst();
            this.recognitionConfirmed = true;
        }
        this.status = MediaAnalysisStatus.COMPLETED;
        this.aiDescription = null;
        this.aiTags = null;
        this.recognitionScore = score;
        this.errorMessage = null;
        this.updatedAt = now;
        return true;
    }

    /**
     * AI 分析回调失败
     *
     * 幂等：已终态直接跳过。
     */
    public void markAnalysisFailed(String errorMessage) {
        if (status.isTerminal()) {
            return;
        }
        this.status = MediaAnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /** 重新发起 AI 分析，兼容历史 skipped 素材和用户主动重试。 */
    public void requestAnalysis(LocalDateTime now) {
        Objects.requireNonNull(now, "分析时间不能为空");
        if (deleted) {
            throw new IllegalStateException("已删除素材不能重新分析");
        }
        if (!recognitionConfirmed) {
            this.petId = null;
        }
        this.status = MediaAnalysisStatus.PENDING;
        this.aiDescription = null;
        this.aiTags = null;
        this.recognitionScore = null;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    /**
     * 确认或修正归属
     *
     * @param petId 新归属；传 null 表示清除归属（清除后 confirmed 复位，等待重新识别）
     */
    public void confirmPet(Long petId) {
        this.petId = petId;
        this.recognitionConfirmed = petId != null;
    }

    /**
     * 编辑标签与拍摄时间（takenAt 为 null 表示不修改）
     */
    public void updateInfo(List<String> tags, LocalDateTime takenAt, LocalDateTime now) {
        // 编辑请求可能带回已存在的自动宠物标签，不能因总数超过手工标签上限而丢失归属。
        this.tags = normalizeTagsWithoutCount(tags);
        if (takenAt != null) {
            this.takenAt = takenAt;
        }
        this.updatedAt = now;
    }

    /**
     * 追加系统自动标签（例如宠物名称）。自动标签允许超过用户手工标签的数量上限，仍校验单个长度。
     */
    public void addTags(List<String> tags) {
        List<String> additions = normalizeTagsWithoutCount(tags);
        if (additions.isEmpty()) {
            return;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(this.tags == null ? List.of() : this.tags);
        merged.addAll(additions);
        this.tags = new ArrayList<>(merged);
    }

    /**
     * 替换素材中的一个标签，供宠物改名后的异步迁移使用。
     */
    public boolean replaceTag(String oldTag, String newTag, LocalDateTime now) {
        if (oldTag == null || newTag == null) {
            return false;
        }
        String oldValue = oldTag.trim();
        String newValue = newTag.trim();
        if (oldValue.isEmpty() || newValue.isEmpty() || oldValue.equals(newValue)
                || this.tags == null || this.tags.isEmpty()) {
            return false;
        }
        boolean changed = false;
        List<String> replaced = new ArrayList<>();
        for (String tag : this.tags) {
            if (oldValue.equals(tag)) {
                replaced.add(newValue);
                changed = true;
            } else {
                replaced.add(tag);
            }
        }
        if (changed) {
            this.tags = new ArrayList<>(new LinkedHashSet<>(replaced));
            this.updatedAt = now;
        }
        return changed;
    }

    /**
     * 宠物删除级联：清空归属（素材保留，confirmed 复位）
     */
    public void clearPetReference() {
        this.petId = null;
        this.recognitionConfirmed = false;
    }

    /**
     * 逻辑删除
     */
    public void delete() {
        this.deleted = true;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    /**
     * 标签规范化：去空、trim、去重（保序）、数量与长度校验
     */
    private static List<String> normalizeTags(List<String> tags) {
        List<String> normalized = normalizeTagsWithoutCount(tags);
        if (normalized.size() > MAX_TAG_COUNT) {
            throw new IllegalArgumentException("标签数量不能超过 " + MAX_TAG_COUNT + " 个");
        }
        return normalized;
    }

    private static List<String> normalizeTagsWithoutCount(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("标签长度不能超过 " + MAX_TAG_LENGTH + " 字符: " + trimmed);
            }
            normalized.add(trimmed);
        }
        return new ArrayList<>(normalized);
    }
}
