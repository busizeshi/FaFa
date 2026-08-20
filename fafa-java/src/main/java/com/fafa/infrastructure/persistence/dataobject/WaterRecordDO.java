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
 * 饮水记录数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("water_record")
public class WaterRecordDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private LocalDateTime waterTime;
    
    private Integer amount;
    
    private String remarks;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
