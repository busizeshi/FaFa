package com.fafa.domain.model.event;

import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderCompletion;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 事件记录（成长档案数据底座）
 *
 * Phase 3 提醒完成写入（source=reminder）；Phase 4 手动创建/编辑（source=manual）。
 * 仅 source=manual 的记录可编辑/删除（保护提醒完成闭环数据）。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public class EventRecord {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_MATERIAL_COUNT = 9;
    private static final int MAX_TAG_COUNT = 10;

    private Long id;
    private final Long petId;
    private final Long userId;
    private final EventSource source;
    /** 来源对象ID（source=reminder 时为 reminderId；manual 时为 null） */
    private final Long sourceId;
    private EventType eventType;
    private String title;
    private String content;
    private LocalDate eventDate;
    private List<String> materialUrls;
    private List<String> tags;
    /** 记录人ID（家庭协同预留） */
    private final Long operatorId;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EventRecord(Long id, Long petId, Long userId, EventSource source, Long sourceId,
                        EventType eventType, String title, String content, LocalDate eventDate,
                        List<String> materialUrls, List<String> tags, Long operatorId,
                        boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.userId = userId;
        this.source = source;
        this.sourceId = sourceId;
        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.eventDate = eventDate;
        this.materialUrls = materialUrls;
        this.tags = tags;
        this.operatorId = operatorId;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 提醒完成生成事件
     *
     * 标题取提醒标题，详情取完成备注，日期取完成日期，素材取完成照片。
     * 仅 type.generatesEvent() 的提醒可调用（白名单校验在应用层）。
     */
    public static EventRecord fromReminder(Reminder reminder, ReminderCompletion completion) {
        Objects.requireNonNull(reminder, "reminder 不能为空");
        Objects.requireNonNull(completion, "completion 不能为空");

        return new EventRecord(null, reminder.getPetId(), reminder.getUserId(),
                EventSource.REMINDER, reminder.getId(),
                mapEventType(reminder.getType()),
                reminder.getTitle(), completion.getNotes(),
                completion.getCompletionTime().toLocalDate(),
                completion.getPhotoUrls(), null, completion.getUserId(),
                false, null, null);
    }

    /**
     * 手动创建事件（成长档案，source=manual，sourceId=null）
     *
     * @param now       当前时间（校验事件日期不晚于今天）
     * @param today     今天日期
     */
    public static EventRecord createManual(Long petId, Long userId, EventType eventType, String title,
                                           String content, LocalDate eventDate,
                                           List<String> materialUrls, List<String> tags,
                                           LocalDateTime now, LocalDate today) {
        return createManual(petId, userId, eventType, title, content, eventDate, materialUrls, tags,
                userId, now, today);
    }

    public static EventRecord createManual(Long petId, Long userId, EventType eventType, String title,
                                           String content, LocalDate eventDate,
                                           List<String> materialUrls, List<String> tags, Long operatorId,
                                           LocalDateTime now, LocalDate today) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(eventType, "eventType 不能为空");
        Objects.requireNonNull(eventDate, "eventDate 不能为空");
        validateTitle(title);
        validateContent(content);
        validateMaterialUrls(materialUrls);
        List<String> normalizedTags = normalizeTags(tags);
        if (eventDate.isAfter(today)) {
            throw new IllegalArgumentException("事件日期不能晚于今天");
        }

        return new EventRecord(null, petId, userId, EventSource.MANUAL, null,
                eventType, title, content, eventDate, materialUrls, normalizedTags, operatorId,
                false, now, now);
    }

    /**
     * 手动编辑（仅 source=manual 可编辑，保护提醒完成闭环数据）
     */
    public void updateManual(EventType eventType, String title, String content, LocalDate eventDate,
                             List<String> materialUrls, List<String> tags,
                             LocalDateTime now, LocalDate today) {
        if (source != EventSource.MANUAL) {
            throw new IllegalStateException("仅手动创建的事件可编辑");
        }
        Objects.requireNonNull(eventType, "eventType 不能为空");
        Objects.requireNonNull(eventDate, "eventDate 不能为空");
        validateTitle(title);
        validateContent(content);
        validateMaterialUrls(materialUrls);
        List<String> normalizedTags = normalizeTags(tags);
        if (eventDate.isAfter(today)) {
            throw new IllegalArgumentException("事件日期不能晚于今天");
        }

        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.eventDate = eventDate;
        this.materialUrls = materialUrls;
        this.tags = normalizedTags;
        this.updatedAt = now;
    }

    /**
     * 手动删除（仅 source=manual 可删除）
     */
    public void deleteManual() {
        if (source != EventSource.MANUAL) {
            throw new IllegalStateException("仅手动创建的事件可删除");
        }
        this.deleted = true;
    }

    /**
     * 是否为手动创建（可编辑/删除）
     */
    public boolean isManual() {
        return source == EventSource.MANUAL;
    }

    /**
     * 仓储重建
     */
    public static EventRecord restore(Long id, Long petId, Long userId, EventSource source, Long sourceId,
                                      EventType eventType, String title, String content, LocalDate eventDate,
                                      List<String> materialUrls, List<String> tags, Long operatorId,
                                      boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new EventRecord(id, petId, userId, source, sourceId, eventType, title, content,
                eventDate, materialUrls, tags, operatorId, deleted, createdAt, updatedAt);
    }

    /**
     * ReminderType → EventType 同名映射（调用前已校验 generatesEvent）
     */
    private static EventType mapEventType(com.fafa.domain.model.reminder.ReminderType type) {
        return switch (type) {
            case VACCINE -> EventType.VACCINE;
            case DEWORMING -> EventType.DEWORMING;
            case BATH -> EventType.BATH;
            case NAIL_TRIM -> EventType.NAIL_TRIM;
            case CHECKUP -> EventType.CHECKUP;
            case CUSTOM -> EventType.CUSTOM;
            default -> throw new IllegalArgumentException("提醒类型不生成事件记录: " + type.getCode());
        };
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("事件标题不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("事件标题不能超过 " + MAX_TITLE_LENGTH + " 字符");
        }
    }

    private static void validateContent(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("事件内容不能超过 " + MAX_CONTENT_LENGTH + " 字符");
        }
    }

    private static void validateMaterialUrls(List<String> materialUrls) {
        if (materialUrls != null && materialUrls.size() > MAX_MATERIAL_COUNT) {
            throw new IllegalArgumentException("事件素材不能超过 " + MAX_MATERIAL_COUNT + " 张");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return null;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.length() > 32) {
                throw new IllegalArgumentException("事件标签不能为空且不能超过32字符");
            }
            normalized.add(trimmed);
        }
        if (normalized.size() > MAX_TAG_COUNT) {
            throw new IllegalArgumentException("事件标签不能超过 " + MAX_TAG_COUNT + " 个");
        }
        return new ArrayList<>(normalized);
    }
}
