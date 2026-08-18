package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.record.RecordId;
import com.fafa.domain.model.record.WaterRecord;
import com.fafa.infrastructure.persistence.dataobject.WaterRecordDO;

/**
 * 饮水记录转换器
 */
public class WaterRecordConverter {
    
    /**
     * DO转领域模型
     */
    public static WaterRecord toDomain(WaterRecordDO waterRecordDO) {
        if (waterRecordDO == null) {
            return null;
        }
        
        return WaterRecord.builder()
                .recordId(RecordId.of(waterRecordDO.getId()))
                .petId(waterRecordDO.getPetId())
                .userId(waterRecordDO.getUserId())
                .waterTime(waterRecordDO.getWaterTime())
                .amount(waterRecordDO.getAmount())
                .remarks(waterRecordDO.getRemarks())
                .build();
    }
    
    /**
     * 领域模型转DO
     */
    public static WaterRecordDO toDO(WaterRecord waterRecord) {
        if (waterRecord == null) {
            return null;
        }
        
        return WaterRecordDO.builder()
                .id(waterRecord.getRecordId() != null ? waterRecord.getRecordId().getValue() : null)
                .petId(waterRecord.getPetId())
                .userId(waterRecord.getUserId())
                .waterTime(waterRecord.getWaterTime())
                .amount(waterRecord.getAmount())
                .remarks(waterRecord.getRemarks())
                .build();
    }
}
