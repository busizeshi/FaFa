package com.fafa.domain.model.medical;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/** 就诊档案与已有媒体素材的关联实体。 */
@Getter
public final class MedicalVisitAttachment {

    private Long id;
    private final Long userId;
    private final Long visitId;
    private final Long mediaId;
    private final String attachmentType;
    private final int sortOrder;
    private LocalDateTime createdAt;

    private MedicalVisitAttachment(Long id, Long userId, Long visitId, Long mediaId,
                                   String attachmentType, int sortOrder,
                                   LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.visitId = visitId;
        this.mediaId = mediaId;
        this.attachmentType = attachmentType;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public static MedicalVisitAttachment create(Long userId, Long visitId, Long mediaId,
                                                String attachmentType, int sortOrder) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(visitId, "visitId 不能为空");
        Objects.requireNonNull(mediaId, "mediaId 不能为空");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("附件排序不能为负");
        }
        String type = attachmentType == null || attachmentType.isBlank()
                ? "other" : attachmentType.trim();
        if (!java.util.Set.of("receipt", "report", "prescription", "other").contains(type)) {
            throw new IllegalArgumentException("附件类型不支持");
        }
        return new MedicalVisitAttachment(null, userId, visitId, mediaId, type,
                sortOrder, null);
    }

    public static MedicalVisitAttachment restore(Long id, Long userId, Long visitId,
                                                 Long mediaId, String attachmentType,
                                                 int sortOrder, LocalDateTime createdAt) {
        return new MedicalVisitAttachment(id, userId, visitId, mediaId, attachmentType,
                sortOrder, createdAt);
    }
}
