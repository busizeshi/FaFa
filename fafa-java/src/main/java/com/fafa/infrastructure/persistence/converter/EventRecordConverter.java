package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.record.EventRecord;
import com.fafa.domain.model.record.RecordId;
import com.fafa.infrastructure.persistence.dataobject.EventRecordDO;

/**
 * 事件记录转换器
 */
public class EventRecordConverter {
    
    /**
     * DO转领域模型
     */
    public static EventRecord toDomain(EventRecordDO eventRecordDO) {
        if (eventRecordDO == null) {
            return null;
        }
        
        return EventRecord.builder()
                .recordId(RecordId.of(eventRecordDO.getId()))
                .petId(eventRecordDO.getPetId())
                .userId(eventRecordDO.getUserId())
                .eventTime(eventRecordDO.getEventTime())
                .eventType(eventRecordDO.getEventType())
                .title(eventRecordDO.getTitle())
                .content(eventRecordDO.getContent())
                .images(eventRecordDO.getImages())
                .remarks(eventRecordDO.getRemarks())
                .build();
    }
    
    /**
     * 领域模型转DO
     */
    public static EventRecordDO toDO(EventRecord eventRecord) {
        if (eventRecord == null) {
            return null;
        }
        
        return EventRecordDO.builder()
                .id(eventRecord.getRecordId() != null ? eventRecord.getRecordId().getValue() : null)
                .petId(eventRecord.getPetId())
                .userId(eventRecord.getUserId())
                .eventTime(eventRecord.getEventTime())
                .eventType(eventRecord.getEventType())
                .title(eventRecord.getTitle())
                .content(eventRecord.getContent())
                .images(eventRecord.getImages())
                .remarks(eventRecord.getRemarks())
                .build();
    }
}
