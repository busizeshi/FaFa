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
 * 喂食记录数据对象
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feed_record")
public class FeedRecordDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private LocalDateTime feedTime;
    
    private String foodName;
    
    private String foodType;
    
    private String amount;
    
    private String unit;
    
    private String brand;
    
    private String remarks;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
