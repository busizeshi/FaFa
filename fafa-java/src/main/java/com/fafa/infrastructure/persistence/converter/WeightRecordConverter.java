package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.record.RecordId;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.infrastructure.persistence.dataobject.WeightRecordDO;

/**
 * 体重记录转换器
 */
public class WeightRecordConverter {
    
    /**
     * DO转领域模型
     */
    public static WeightRecord toDomain(WeightRecordDO weightRecordDO) {
        if (weightRecordDO == null) {
            return null;
        }
        
        return WeightRecord.builder()
                .recordId(RecordId.of(weightRecordDO.getId()))
                .petId(weightRecordDO.getPetId())
                .userId(weightRecordDO.getUserId())
                .recordDate(weightRecordDO.getRecordDate())
                .weight(weightRecordDO.getWeight())
                .bcsScore(weightRecordDO.getBcsScore())
                .remarks(weightRecordDO.getRemarks())
                .build();
    }
    
    /**
     * 领域模型转DO
     */
    public static WeightRecordDO toDO(WeightRecord weightRecord) {
        if (weightRecord == null) {
            return null;
        }
        
        return WeightRecordDO.builder()
                .id(weightRecord.getRecordId() != null ? weightRecord.getRecordId().getValue() : null)
                .petId(weightRecord.getPetId())
                .userId(weightRecord.getUserId())
                .recordDate(weightRecord.getRecordDate())
                .weight(weightRecord.getWeight())
                .bcsScore(weightRecord.getBcsScore())
                .remarks(weightRecord.getRemarks())
                .build();
    }
}
