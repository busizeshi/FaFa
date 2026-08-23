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
 * 一行 = 一个照片或视频素材 + AI 分析状态 + 宠物归属识别。
 *
 * 业务规则（见 phase4-implementation-plan.md 4.2）：
 * 1. photo 落库 status=pending（等 AI 回调）；video 落库 status=skipped（不进 AI 链路）
 * 2. tags 为标签名快照：去重去空、≤10 个、单个 ≤32 字符；标签定义删除不影响快照
 * 3. 上传指定 petId → recognitionConfirmed=true、recognitionScore=null（用户明确归属）
 * 4. AI 识别归属回填仅当「petId 为空且未确认」；用户已指定归属不覆盖
 * 5. 已终态（completed/failed/skipped）时重复回调幂等跳过
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public class MediaAsset {

    /** 素材标签数量上限 */
    public static final int MAX_TAG_COUNT = 10;
    /** 单个标签最大长度（素材 tags 快照口径） */
    public static final int MAX_TAG_LENGTH = 32;
    /** AI 描述最大长度 */
    public static final int MAX_AI_DESCRIPTION_LENGTH = 1000;

    private Long id;
    private final Long userId;
    /** 当前归属宠物（可空；识别回填待确认 / 用户指定即确认） */
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

    private MediaAsset(Long id, Long userId, Long petId, MediaType mediaType, String fileUrl,
                       String fileName, long fileSize, String mimeType, List<String> tags,
                       MediaAnalysisStatus status, String aiDescription, List<String> aiTags,
                       BigDecimal recognitionScore, boolean recognitionConfirmed,
                       LocalDateTime takenAt, String errorMessage, boolean deleted,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
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
        MediaAnalysisStatus initialStatus = mediaType == MediaType.PHOTO
                ? MediaAnalysisStatus.PENDING
                : MediaAnalysisStatus.SKIPPED;

        return new MediaAsset(null, userId, petId, mediaType, fileUrl, fileName, fileSize, mimeType,
                normalizedTags, initialStatus, null, null, null, confirmed,
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
        return new MediaAsset(id, userId, petId, mediaType, fileUrl, fileName, fileSize, mimeType,
                tags, status, aiDescription, aiTags, recognitionScore, recognitionConfirmed,
                takenAt, errorMessage, deleted, createdAt, updatedAt);
    }

    /**
     * AI 分析回调成功
     *
     * 幂等：已终态直接跳过。识别归属仅在「petId 为空且未确认」时回填（confirmed 保持 false 待确认）；
     * 用户已指定归属则只更新相似度，不覆盖归属。
     */
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
        this.tags = normalizeTags(tags);
        if (takenAt != null) {
            this.takenAt = takenAt;
        }
        this.updatedAt = now;
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

    /**
     * 标签规范化：去空、trim、去重（保序）、数量与长度校验
     */
    private static List<String> normalizeTags(List<String> tags) {
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
        if (normalized.size() > MAX_TAG_COUNT) {
            throw new IllegalArgumentException("标签数量不能超过 " + MAX_TAG_COUNT + " 个");
        }
        return new ArrayList<>(normalized);
    }
}
