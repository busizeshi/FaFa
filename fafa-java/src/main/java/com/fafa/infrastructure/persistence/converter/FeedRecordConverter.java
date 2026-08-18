package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.record.FeedRecord;
import com.fafa.domain.model.record.RecordId;
import com.fafa.infrastructure.persistence.dataobject.FeedRecordDO;
import org.springframework.stereotype.Component;

/**
 * 喂食记录转换器
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Component
public class FeedRecordConverter {
    
    /**
     * DO 转 领域模型
     */
    public FeedRecord toDomain(FeedRecordDO feedRecordDO) {
        if (feedRecordDO == null) {
            return null;
        }
        
        return FeedRecord.builder()
                .recordId(RecordId.of(feedRecordDO.getId()))
                .petId(feedRecordDO.getPetId())
                .userId(feedRecordDO.getUserId())
                .feedTime(feedRecordDO.getFeedTime())
                .foodName(feedRecordDO.getFoodName())
                .foodType(feedRecordDO.getFoodType())
                .amount(feedRecordDO.getAmount())
                .unit(feedRecordDO.getUnit())
                .brand(feedRecordDO.getBrand())
                .remarks(feedRecordDO.getRemarks())
                .createdAt(feedRecordDO.getCreatedAt())
                .updatedAt(feedRecordDO.getUpdatedAt())
                .build();
    }
    
    /**
     * 领域模型 转 DO
     */
    public FeedRecordDO toDO(FeedRecord feedRecord) {
        if (feedRecord == null) {
            return null;
        }
        
        return FeedRecordDO.builder()
                .id(feedRecord.getRecordId() != null ? feedRecord.getRecordId().getValue() : null)
                .petId(feedRecord.getPetId())
                .userId(feedRecord.getUserId())
                .feedTime(feedRecord.getFeedTime())
                .foodName(feedRecord.getFoodName())
                .foodType(feedRecord.getFoodType())
                .amount(feedRecord.getAmount())
                .unit(feedRecord.getUnit())
                .brand(feedRecord.getBrand())
                .remarks(feedRecord.getRemarks())
                .createdAt(feedRecord.getCreatedAt())
                .updatedAt(feedRecord.getUpdatedAt())
                .build();
    }
}
