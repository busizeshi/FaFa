package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日状态记录持久化对象
 *
 * 一天一格 upsert 语义，物理删除（无逻辑删除列）。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Data
@TableName("daily_status")
public class DailyStatusPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private LocalDate recordDate;

    /** 食欲: excellent/normal/decreased/refused */
    private String appetite;

    /** 精神活动: active/normal/lethargic */
    private String energy;

    /** 饮水程度: normal/more/less/not_noticed */
    private String waterIntake;

    /** 排便: normal/soft/diarrhea/constipation/not_seen */
    private String defecation;

    /** 呕吐: none/once/multiple */
    private String vomiting;

    /** 排尿: normal/abnormal/not_seen */
    private String urination;

    private BigDecimal appetiteAmount;

    private BigDecimal waterAmount;

    private String notes;

    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
