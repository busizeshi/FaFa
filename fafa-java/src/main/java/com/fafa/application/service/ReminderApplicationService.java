package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fafa.application.dto.reminder.CompleteReminderRequest;
import com.fafa.application.dto.reminder.CreateReminderRequest;
import com.fafa.application.dto.reminder.ReminderResponse;
import com.fafa.application.dto.reminder.UpdateReminderRequest;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderId;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 提醒应用服务
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Slf4j
@Service
public class ReminderApplicationService {
    
    @Resource
    private ReminderRepository reminderRepository;
    
    @Resource
    private PetRepository petRepository;
    
    /**
     * 创建提醒
     */
    @Transactional
    public Long createReminder(Long userId, CreateReminderRequest request) {
        // 1. 验证宠物归属
        Optional<Pet> petOpt = petRepository.findById(PetId.of(request.getPetId()));
        if (petOpt.isEmpty()) {
            throw new BusinessException("宠物不存在");
        }
        Pet pet = petOpt.get();
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 2. 创建提醒
        Reminder reminder = Reminder.create(
                request.getPetId(),
                userId,
                request.getTitle(),
                request.getReminderType(),
                request.getRemindTime(),
                request.getRepeatType(),
                request.getRepeatConfig(),
                request.getAdvanceMinutes(),
                request.getDescription()
        );
        
        // 3. 保存
        Reminder savedReminder = reminderRepository.save(reminder);
        
        log.info("创建提醒成功, reminderId: {}, petId: {}, title: {}", 
                savedReminder.getReminderId().getValue(), request.getPetId(), request.getTitle());
        
        return savedReminder.getReminderId().getValue();
    }
    
    /**
     * 查询提醒列表（按宠物）
     */
    public List<ReminderResponse> listRemindersByPet(Long userId, Long petId, String status, 
                                                      Integer pageNum, Integer pageSize) {
        // 1. 验证宠物归属
        Optional<Pet> petOpt = petRepository.findById(PetId.of(petId));
        if (petOpt.isEmpty()) {
            throw new BusinessException("宠物不存在");
        }
        Pet pet = petOpt.get();
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该宠物");
        }
        
        // 2. 查询提醒列表
        List<Reminder> reminders = reminderRepository.findByPetId(petId, status, pageNum, pageSize);
        
        // 3. 转换为响应
        return reminders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询提醒列表（按用户）
     */
    public List<ReminderResponse> listRemindersByUser(Long userId, String status, 
                                                       Integer pageNum, Integer pageSize) {
        List<Reminder> reminders = reminderRepository.findByUserId(userId, status, pageNum, pageSize);
        
        return reminders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询提醒详情
     */
    public ReminderResponse getReminderDetail(Long userId, Long reminderId) {
        // 1. 查询提醒
        Optional<Reminder> reminderOpt = reminderRepository.findById(ReminderId.of(reminderId));
        if (reminderOpt.isEmpty()) {
            throw new BusinessException("提醒不存在");
        }
        Reminder reminder = reminderOpt.get();
        
        // 2. 权限校验
        if (!reminder.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该提醒");
        }
        
        return toResponse(reminder);
    }
    
    /**
     * 更新提醒
     */
    @Transactional
    public void updateReminder(Long userId, Long reminderId, UpdateReminderRequest request) {
        // 1. 查询提醒
        Optional<Reminder> reminderOpt = reminderRepository.findById(ReminderId.of(reminderId));
        if (reminderOpt.isEmpty()) {
            throw new BusinessException("提醒不存在");
        }
        Reminder reminder = reminderOpt.get();
        
        // 2. 权限校验
        if (!reminder.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该提醒");
        }
        
        // 3. 状态校验
        if (!reminder.canBeEdited()) {
            throw new BusinessException("提醒状态不允许编辑");
        }
        
        // 4. 更新
        reminder.update(
                request.getTitle(),
                request.getReminderType(),
                request.getRemindTime(),
                request.getRepeatType(),
                request.getRepeatConfig(),
                request.getAdvanceMinutes(),
                request.getDescription()
        );
        
        reminderRepository.save(reminder);
        
        log.info("更新提醒成功, reminderId: {}", reminderId);
    }
    
    /**
     * 删除提醒
     */
    @Transactional
    public void deleteReminder(Long userId, Long reminderId) {
        // 1. 查询提醒
        Optional<Reminder> reminderOpt = reminderRepository.findById(ReminderId.of(reminderId));
        if (reminderOpt.isEmpty()) {
            throw new BusinessException("提醒不存在");
        }
        Reminder reminder = reminderOpt.get();
        
        // 2. 权限校验
        if (!reminder.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该提醒");
        }
        
        // 3. 删除
        reminderRepository.deleteById(ReminderId.of(reminderId));
        
        log.info("删除提醒成功, reminderId: {}", reminderId);
    }
    
    /**
     * 完成提醒
     */
    @Transactional
    public void completeReminder(Long userId, Long reminderId, CompleteReminderRequest request) {
        // 1. 查询提醒
        Optional<Reminder> reminderOpt = reminderRepository.findById(ReminderId.of(reminderId));
        if (reminderOpt.isEmpty()) {
            throw new BusinessException("提醒不存在");
        }
        Reminder reminder = reminderOpt.get();
        
        // 2. 权限校验
        if (!reminder.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该提醒");
        }
        
        // 3. 状态校验
        if (!reminder.canBeCompleted()) {
            throw new BusinessException("提醒状态不允许完成");
        }
        
        // 4. 完成提醒
        reminder.complete(request.getCompletionNote(), request.getCompletionImages());
        reminderRepository.save(reminder);
        
        log.info("完成提醒成功, reminderId: {}", reminderId);
        
        // 5. 如果是重复提醒，创建下一次提醒
        if (reminder.isRepeating()) {
            LocalDateTime nextTime = calculateNextRemindTime(reminder);
            if (nextTime != null) {
                Reminder nextReminder = Reminder.create(
                        reminder.getPetId(),
                        reminder.getUserId(),
                        reminder.getTitle(),
                        reminder.getReminderType(),
                        nextTime,
                        reminder.getRepeatType(),
                        reminder.getRepeatConfig(),
                        reminder.getAdvanceMinutes(),
                        reminder.getDescription()
                );
                reminderRepository.save(nextReminder);
                
                log.info("创建下一次重复提醒成功, nextReminderId: {}, nextTime: {}", 
                        nextReminder.getReminderId().getValue(), nextTime);
            }
        }
    }
    
    /**
     * 取消提醒
     */
    @Transactional
    public void cancelReminder(Long userId, Long reminderId) {
        // 1. 查询提醒
        Optional<Reminder> reminderOpt = reminderRepository.findById(ReminderId.of(reminderId));
        if (reminderOpt.isEmpty()) {
            throw new BusinessException("提醒不存在");
        }
        Reminder reminder = reminderOpt.get();
        
        // 2. 权限校验
        if (!reminder.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该提醒");
        }
        
        // 3. 取消
        reminder.cancel();
        reminderRepository.save(reminder);
        
        log.info("取消提醒成功, reminderId: {}", reminderId);
    }
    
    /**
     * 统计提醒数量（按宠物）
     */
    public int countRemindersByPet(Long userId, Long petId, String status) {
        // 验证宠物归属
        Optional<Pet> petOpt = petRepository.findById(PetId.of(petId));
        if (petOpt.isEmpty()) {
            throw new BusinessException("宠物不存在");
        }
        Pet pet = petOpt.get();
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该宠物");
        }
        
        return reminderRepository.countByPetId(petId, status);
    }
    
    /**
     * 统计提醒数量（按用户）
     */
    public int countRemindersByUser(Long userId, String status) {
        return reminderRepository.countByUserId(userId, status);
    }
    
    /**
     * 计算下一次提醒时间
     */
    private LocalDateTime calculateNextRemindTime(Reminder reminder) {
        LocalDateTime currentTime = reminder.getRemindTime();
        String repeatType = reminder.getRepeatType();
        
        switch (repeatType) {
            case "daily":
                return currentTime.plusDays(1);
                
            case "weekly":
                return currentTime.plusWeeks(1);
                
            case "monthly":
                return currentTime.plusMonths(1);
                
            case "custom":
                // 自定义重复逻辑
                if (reminder.getRepeatConfig() != null) {
                    JSONObject config = JSONUtil.parseObj(reminder.getRepeatConfig());
                    if (config.containsKey("days")) {
                        // 按周几重复
                        List<Integer> days = config.getBeanList("days", Integer.class);
                        LocalDateTime next = currentTime.plusDays(1);
                        for (int i = 0; i < 7; i++) {
                            int dayOfWeek = next.getDayOfWeek().getValue(); // 1-7 (周一到周日)
                            if (days.contains(dayOfWeek)) {
                                return next;
                            }
                            next = next.plusDays(1);
                        }
                    }
                }
                return null;
                
            default:
                return null;
        }
    }
    
    /**
     * 转换为响应
     */
    private ReminderResponse toResponse(Reminder reminder) {
        return ReminderResponse.builder()
                .id(reminder.getReminderId().getValue())
                .petId(reminder.getPetId())
                .userId(reminder.getUserId())
                .title(reminder.getTitle())
                .reminderType(reminder.getReminderType())
                .remindTime(reminder.getRemindTime())
                .repeatType(reminder.getRepeatType())
                .repeatConfig(reminder.getRepeatConfig())
                .advanceMinutes(reminder.getAdvanceMinutes())
                .description(reminder.getDescription())
                .status(reminder.getStatus())
                .completedAt(reminder.getCompletedAt())
                .completionNote(reminder.getCompletionNote())
                .completionImages(reminder.getCompletionImages())
                .isNotified(reminder.getIsNotified())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
}
