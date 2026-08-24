package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_visit_attachment")
public class MedicalVisitAttachmentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long visitId;
    private Long mediaId;
    private String attachmentType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
