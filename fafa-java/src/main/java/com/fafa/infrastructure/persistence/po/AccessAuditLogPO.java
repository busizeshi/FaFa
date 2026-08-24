package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_audit_log")
public class AccessAuditLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long petId;
    private Long userId;
    private Long operatorId;
    private String operation;
    private String resourceType;
    private String resourceId;
    private String result;
    private String reason;
    @TableField("metadata_json")
    private String metadataJson;
    private LocalDateTime createdAt;
}
