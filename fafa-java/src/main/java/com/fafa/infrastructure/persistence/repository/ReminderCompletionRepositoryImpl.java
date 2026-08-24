package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.reminder.ReminderCompletion;
import com.fafa.domain.repository.ReminderCompletionRepository;
import com.fafa.infrastructure.persistence.mapper.ReminderCompletionHistoryMapper;
import com.fafa.infrastructure.persistence.po.ReminderCompletionHistoryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒完成历史仓储实现（追加只写）
 *
 * @author FaFa Team
 * @since 3.0
 */
@Repository
@RequiredArgsConstructor
public class ReminderCompletionRepositoryImpl implements ReminderCompletionRepository {

    private final ReminderCompletionHistoryMapper completionMapper;
    private final JsonListConverter jsonListConverter;

    @Override
    public ReminderCompletion save(ReminderCompletion completion) {
        ReminderCompletionHistoryPO po = toPO(completion);

        if (po.getId() == null) {
            completionMapper.insert(po);
        } else {
            completionMapper.updateById(po);
        }

        return toDomain(completionMapper.selectById(po.getId()));
    }

    @Override
    public List<ReminderCompletion> findByReminderId(Long reminderId) {
        LambdaQueryWrapper<ReminderCompletionHistoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderCompletionHistoryPO::getReminderId, reminderId)
               .orderByDesc(ReminderCompletionHistoryPO::getCompletionTime);
        return completionMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ReminderCompletion> findByUserIdAndTimeBetween(Long userId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<ReminderCompletionHistoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderCompletionHistoryPO::getUserId, userId)
               .ge(ReminderCompletionHistoryPO::getCompletionTime, start)
               .lt(ReminderCompletionHistoryPO::getCompletionTime, end)
               .orderByDesc(ReminderCompletionHistoryPO::getCompletionTime);
        return completionMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ReminderCompletion> findByPetIdAndTimeBetween(Long userId, Long petId,
                                                               LocalDateTime start,
                                                               LocalDateTime end) {
        LambdaQueryWrapper<ReminderCompletionHistoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderCompletionHistoryPO::getUserId, userId)
                .eq(ReminderCompletionHistoryPO::getPetId, petId)
                .ge(ReminderCompletionHistoryPO::getCompletionTime, start)
                .lt(ReminderCompletionHistoryPO::getCompletionTime, end)
                .orderByDesc(ReminderCompletionHistoryPO::getCompletionTime);
        return completionMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 持久化对象转领域对象
     */
    private ReminderCompletion toDomain(ReminderCompletionHistoryPO po) {
        if (po == null) {
            return null;
        }
        return ReminderCompletion.restore(
                po.getId(),
                po.getReminderId(),
                po.getPetId(),
                po.getUserId(),
                po.getReminderTitle(),
                po.getReminderType(),
                po.getCompletionTime(),
                po.getNotes(),
                jsonListConverter.fromJson(po.getPhotoUrls()),
                po.getEventRecordId(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private ReminderCompletionHistoryPO toPO(ReminderCompletion completion) {
        ReminderCompletionHistoryPO po = new ReminderCompletionHistoryPO();
        po.setId(completion.getId());
        po.setReminderId(completion.getReminderId());
        po.setPetId(completion.getPetId());
        po.setUserId(completion.getUserId());
        po.setReminderTitle(completion.getReminderTitle());
        po.setReminderType(completion.getReminderType());
        po.setCompletionTime(completion.getCompletionTime());
        po.setNotes(completion.getNotes());
        po.setPhotoUrls(jsonListConverter.toJson(completion.getPhotoUrls()));
        po.setEventRecordId(completion.getEventRecordId());
        return po;
    }
}
