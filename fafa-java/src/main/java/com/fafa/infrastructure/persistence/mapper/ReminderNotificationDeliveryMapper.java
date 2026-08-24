package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ReminderNotificationDeliveryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReminderNotificationDeliveryMapper extends BaseMapper<ReminderNotificationDeliveryPO> {
    @Select("SELECT * FROM reminder_notification_delivery WHERE reminder_id=#{reminderId} "
            + "AND recipient_user_id=#{recipientUserId} AND delivery_window=#{deliveryWindow} LIMIT 1")
    ReminderNotificationDeliveryPO selectByBusinessKey(@Param("reminderId") Long reminderId,
                                                        @Param("recipientUserId") Long recipientUserId,
                                                        @Param("deliveryWindow") String deliveryWindow);
}
