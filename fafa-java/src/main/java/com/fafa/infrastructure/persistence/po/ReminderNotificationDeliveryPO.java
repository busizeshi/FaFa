package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reminder_notification_delivery")
public class ReminderNotificationDeliveryPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reminderId;
    private Long petId;
    private Long recipientUserId;
    private String deliveryWindow;
    private String status;
    private String providerMessageId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime pushedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
