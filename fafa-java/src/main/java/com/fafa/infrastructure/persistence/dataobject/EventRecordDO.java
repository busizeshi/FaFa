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
 * 事件记录数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("event_record")
public class EventRecordDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private LocalDateTime eventTime;
    
    private String eventType;
    
    private String title;
    
    private String content;
    
    private String images;
    
    private String remarks;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private Integer isDeleted;
}
