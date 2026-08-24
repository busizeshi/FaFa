package com.fafa.infrastructure.persistence.repository;

import com.fafa.domain.model.reminder.DeliveryStatus;
import com.fafa.domain.model.reminder.ReminderNotificationDelivery;
import com.fafa.domain.repository.ReminderNotificationDeliveryRepository;
import com.fafa.infrastructure.persistence.mapper.ReminderNotificationDeliveryMapper;
import com.fafa.infrastructure.persistence.po.ReminderNotificationDeliveryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReminderNotificationDeliveryRepositoryImpl implements ReminderNotificationDeliveryRepository {
    private final ReminderNotificationDeliveryMapper mapper;

    @Override
    public boolean createPendingIfAbsent(ReminderNotificationDelivery delivery) {
        ReminderNotificationDeliveryPO existing = mapper.selectByBusinessKey(delivery.getReminderId(), delivery.getRecipientUserId(), delivery.getDeliveryWindow());
        if (existing != null) return false;
        try {
            ReminderNotificationDeliveryPO po = toPO(delivery);
            mapper.insert(po);
            delivery.assignId(po.getId());
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public Optional<ReminderNotificationDelivery> findByBusinessKey(Long reminderId, Long recipientUserId,
                                                                      String deliveryWindow) {
        ReminderNotificationDeliveryPO po = mapper.selectByBusinessKey(reminderId, recipientUserId, deliveryWindow);
        return po == null ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public ReminderNotificationDelivery save(ReminderNotificationDelivery delivery) {
        ReminderNotificationDeliveryPO po = toPO(delivery);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    private ReminderNotificationDelivery toDomain(ReminderNotificationDeliveryPO po) {
        return ReminderNotificationDelivery.restore(po.getId(), po.getReminderId(), po.getPetId(), po.getRecipientUserId(),
                po.getDeliveryWindow(), DeliveryStatus.valueOf(po.getStatus().toUpperCase()), po.getProviderMessageId(),
                po.getErrorMessage(), po.getRetryCount() == null ? 0 : po.getRetryCount(), po.getPushedAt(),
                po.getCreatedAt(), po.getUpdatedAt());
    }

    private ReminderNotificationDeliveryPO toPO(ReminderNotificationDelivery delivery) {
        ReminderNotificationDeliveryPO po = new ReminderNotificationDeliveryPO();
        po.setId(delivery.getId()); po.setReminderId(delivery.getReminderId()); po.setPetId(delivery.getPetId());
        po.setRecipientUserId(delivery.getRecipientUserId()); po.setDeliveryWindow(delivery.getDeliveryWindow());
        po.setStatus(delivery.getStatus().getCode()); po.setProviderMessageId(delivery.getProviderMessageId());
        po.setErrorMessage(delivery.getErrorMessage()); po.setRetryCount(delivery.getRetryCount());
        po.setPushedAt(delivery.getPushedAt());
        return po;
    }
}
