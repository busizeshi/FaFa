package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderId;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.infrastructure.persistence.converter.ReminderConverter;
import com.fafa.infrastructure.persistence.dataobject.ReminderDO;
import com.fafa.infrastructure.persistence.mapper.ReminderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 提醒仓储实现
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Repository
public class ReminderRepositoryImpl implements ReminderRepository {
    
    @Resource
    private ReminderMapper reminderMapper;
    
    @Resource
    private ReminderConverter reminderConverter;
    
    @Override
    public Reminder save(Reminder reminder) {
        ReminderDO reminderDO = reminderConverter.toDO(reminder);
        
        if (reminderDO.getId() == null) {
            // 新增
            reminderMapper.insert(reminderDO);
        } else {
            // 更新
            reminderMapper.updateById(reminderDO);
        }
        
        return reminderConverter.toDomain(reminderDO);
    }
    
    @Override
    public Optional<Reminder> findById(ReminderId reminderId) {
        ReminderDO reminderDO = reminderMapper.selectById(reminderId.getValue());
        return Optional.ofNullable(reminderConverter.toDomain(reminderDO));
    }
    
    @Override
    public List<Reminder> findByPetId(Long petId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ReminderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderDO::getPetId, petId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReminderDO::getStatus, status);
        }
        
        wrapper.orderByDesc(ReminderDO::getRemindTime);
        
        if (pageNum != null && pageSize != null) {
            Page<ReminderDO> page = new Page<>(pageNum, pageSize);
            Page<ReminderDO> result = reminderMapper.selectPage(page, wrapper);
            return result.getRecords().stream()
                    .map(reminderConverter::toDomain)
                    .collect(Collectors.toList());
        } else {
            List<ReminderDO> list = reminderMapper.selectList(wrapper);
            return list.stream()
                    .map(reminderConverter::toDomain)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public List<Reminder> findByUserId(Long userId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ReminderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderDO::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReminderDO::getStatus, status);
        }
        
        wrapper.orderByDesc(ReminderDO::getRemindTime);
        
        if (pageNum != null && pageSize != null) {
            Page<ReminderDO> page = new Page<>(pageNum, pageSize);
            Page<ReminderDO> result = reminderMapper.selectPage(page, wrapper);
            return result.getRecords().stream()
                    .map(reminderConverter::toDomain)
                    .collect(Collectors.toList());
        } else {
            List<ReminderDO> list = reminderMapper.selectList(wrapper);
            return list.stream()
                    .map(reminderConverter::toDomain)
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public List<Reminder> findPendingNotifications(LocalDateTime startTime, LocalDateTime endTime) {
        List<ReminderDO> list = reminderMapper.findPendingNotifications(startTime, endTime);
        return list.stream()
                .map(reminderConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public int countByPetId(Long petId, String status) {
        return reminderMapper.countByPetId(petId, status);
    }
    
    @Override
    public int countByUserId(Long userId, String status) {
        return reminderMapper.countByUserId(userId, status);
    }
    
    @Override
    public void deleteById(ReminderId reminderId) {
        reminderMapper.deleteById(reminderId.getValue());
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<ReminderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReminderDO::getPetId, petId);
        reminderMapper.delete(wrapper);
    }
}
