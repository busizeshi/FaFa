package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fafa.domain.model.reminder.IntervalType;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderStatus;
import com.fafa.domain.model.reminder.ReminderType;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.infrastructure.persistence.mapper.ReminderMapper;
import com.fafa.infrastructure.persistence.po.ReminderPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒仓储实现
 *
 * @author FaFa Team
 * @since 3.0
 */
@Repository
@RequiredArgsConstructor
public class ReminderRepositoryImpl implements ReminderRepository {

    private final ReminderMapper reminderMapper;

    @Override
    public Reminder save(Reminder reminder) {
        ReminderPO po = toPO(reminder);

        if (po.getId() == null) {
            reminderMapper.insert(po);
        } else {
            reminderMapper.updateById(po);
        }

        return toDomain(reminderMapper.selectById(po.getId()));
    }

    @Override
    public Optional<Reminder> findById(Long id) {
        return Optional.ofNullable(toDomain(reminderMapper.selectById(id)));
    }

    @Override
    public List<Reminder> findByPetId(Long petId) {
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getPetId, petId)
               .orderByAsc(ReminderPO::getNextRemindTime);
        return reminderMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Reminder> findByMedicationPlanId(Long medicationPlanId) {
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getMedicationId, medicationPlanId)
                .orderByAsc(ReminderPO::getNextRemindTime);
        return reminderMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Reminder> findDueByUserId(Long userId, LocalDateTime endExclusive) {
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getUserId, userId)
               .eq(ReminderPO::getStatus, ReminderStatus.PENDING.getCode())
               .lt(ReminderPO::getNextRemindTime, endExclusive)
               .orderByAsc(ReminderPO::getNextRemindTime);
        return reminderMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Reminder> findPendingBetween(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getStatus, ReminderStatus.PENDING.getCode())
               .ge(ReminderPO::getNextRemindTime, start)
               .le(ReminderPO::getNextRemindTime, end);
        return reminderMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        // @TableLogic 自动转为逻辑删除
        reminderMapper.deleteById(id);
    }

    @Override
    public void deleteByPetId(Long petId) {
        // @TableLogic 下 delete(wrapper) 即批量逻辑删除
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getPetId, petId);
        reminderMapper.delete(wrapper);
    }

    @Override
    public void deleteByMedicationPlanId(Long medicationPlanId) {
        LambdaQueryWrapper<ReminderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderPO::getMedicationId, medicationPlanId);
        reminderMapper.delete(wrapper);
    }

    @Override
    public void updateLastPushTime(Long id, LocalDateTime pushTime) {
        LambdaUpdateWrapper<ReminderPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReminderPO::getId, id)
               .set(ReminderPO::getLastPushTime, pushTime);
        reminderMapper.update(null, wrapper);
    }

    /**
     * 持久化对象转领域对象
     */
    private Reminder toDomain(ReminderPO po) {
        if (po == null) {
            return null;
        }
        return Reminder.restore(
                po.getId(),
                po.getPetId(),
                po.getUserId(),
                ReminderType.fromCode(po.getType()),
                po.getTitle(),
                po.getDescription(),
                IntervalType.fromCode(po.getIntervalType()),
                po.getIntervalValue(),
                po.getNextRemindTime(),
                ReminderStatus.fromCode(po.getStatus()),
                po.getLastCompletionTime(),
                po.getLastCompletionNotes(),
                po.getLastPushTime(),
                po.getMedicationId(),
                po.getIsDeleted() != null && po.getIsDeleted(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private ReminderPO toPO(Reminder reminder) {
        ReminderPO po = new ReminderPO();
        po.setId(reminder.getId());
        po.setPetId(reminder.getPetId());
        po.setUserId(reminder.getUserId());
        po.setType(reminder.getType().getCode());
        po.setTitle(reminder.getTitle());
        po.setDescription(reminder.getDescription());
        po.setIntervalType(reminder.getIntervalType().getCode());
        po.setIntervalValue(reminder.getIntervalValue());
        po.setNextRemindTime(reminder.getNextRemindTime());
        po.setStatus(reminder.getStatus().getCode());
        po.setLastCompletionTime(reminder.getLastCompletionTime());
        po.setLastCompletionNotes(reminder.getLastCompletionNotes());
        po.setLastPushTime(reminder.getLastPushTime());
        po.setMedicationId(reminder.getMedicationId());
        po.setIsDeleted(reminder.isDeleted());
        return po;
    }
}
