package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体重记录数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("weight_record")
public class WeightRecordDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private LocalDate recordDate;
    
    private BigDecimal weight;
    
    private Integer bcsScore;
    
    private String remarks;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
