package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.record.ExcretionRecord;
import com.fafa.domain.model.record.RecordId;
import com.fafa.infrastructure.persistence.dataobject.ExcretionRecordDO;

/**
 * 排便记录转换器
 */
public class ExcretionRecordConverter {
    
    /**
     * DO转领域模型
     */
    public static ExcretionRecord toDomain(ExcretionRecordDO excretionRecordDO) {
        if (excretionRecordDO == null) {
            return null;
        }
        
        return ExcretionRecord.builder()
                .recordId(RecordId.of(excretionRecordDO.getId()))
                .petId(excretionRecordDO.getPetId())
                .userId(excretionRecordDO.getUserId())
                .excretionTime(excretionRecordDO.getExcretionTime())
                .type(excretionRecordDO.getType())
                .color(excretionRecordDO.getColor())
                .shape(excretionRecordDO.getShape())
                .abnormal(excretionRecordDO.getAbnormal())
                .remarks(excretionRecordDO.getRemarks())
                .build();
    }
    
    /**
     * 领域模型转DO
     */
    public static ExcretionRecordDO toDO(ExcretionRecord excretionRecord) {
        if (excretionRecord == null) {
            return null;
        }
        
        return ExcretionRecordDO.builder()
                .id(excretionRecord.getRecordId() != null ? excretionRecord.getRecordId().getValue() : null)
                .petId(excretionRecord.getPetId())
                .userId(excretionRecord.getUserId())
                .excretionTime(excretionRecord.getExcretionTime())
                .type(excretionRecord.getType())
                .color(excretionRecord.getColor())
                .shape(excretionRecord.getShape())
                .abnormal(excretionRecord.getAbnormal())
                .remarks(excretionRecord.getRemarks())
                .build();
    }
}
