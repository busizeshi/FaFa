package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 事件记录聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecord {
    
    /**
     * 记录ID
     */
    private RecordId recordId;
    
    /**
     * 宠物ID
     */
    private Long petId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 事件时间
     */
    private LocalDateTime eventTime;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件标题
     */
    private String title;
    
    /**
     * 事件内容
     */
    private String content;
    
    /**
     * 图片（多张以逗号分隔）
     */
    private String images;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建事件记录
     */
    public static EventRecord create(Long petId, Long userId, LocalDateTime eventTime, 
                                     String eventType, String title) {
        return EventRecord.builder()
                .petId(petId)
                .userId(userId)
                .eventTime(eventTime)
                .eventType(eventType)
                .title(title)
                .build();
    }
    
    /**
     * 更新事件信息
     */
    public void updateEventInfo(LocalDateTime eventTime, String eventType, String title, 
                                String content, String images, String remarks) {
        this.eventTime = eventTime;
        this.eventType = eventType;
        this.title = title;
        this.content = content;
        this.images = images;
        this.remarks = remarks;
    }
}
