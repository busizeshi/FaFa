package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 开支记录持久化对象。 */
@Data
@TableName("expense_record")
public class ExpenseRecordPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private String category;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private String title;
    private String notes;
    private Long receiptMediaId;
    private Long foodCardId;
    private String foodNameSnapshot;
    private Long medicalVisitId;
    private String sourceType;
    private Boolean systemGenerated;
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
