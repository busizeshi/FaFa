package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 排便记录数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("excretion_record")
public class ExcretionRecordDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private LocalDateTime excretionTime;
    
    private String type;
    
    private String color;
    
    private String shape;
    
    private Boolean abnormal;
    
    private String remarks;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
