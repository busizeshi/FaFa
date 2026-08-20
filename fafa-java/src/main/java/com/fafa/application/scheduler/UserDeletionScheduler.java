package com.fafa.application.scheduler;

import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserStatus;
import com.fafa.domain.repository.*;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import com.fafa.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户注销定时任务
 * 每天凌晨2点执行，删除冷静期超过7天的用户数据
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {
    
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final FeedRecordRepository feedRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final ExcretionRecordRepository excretionRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderRepository reminderRepository;
    private final PhotoRepository photoRepository;
    
    /**
     * 每天凌晨2点执行
     * 删除冷静期超过7天的待注销用户
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredDeletionRequests() {
        log.info("开始执行用户注销定时任务");
        
        try {
            // 查询所有待注销且超过7天的用户
            LocalDateTime expirationTime = LocalDateTime.now().minusDays(7);
            List<UserDO> expiredUsers = userMapper.findExpiredDeletionRequests(
                    UserStatus.PENDING_DELETION.getCode(),
                    expirationTime
            );
            
            if (expiredUsers.isEmpty()) {
                log.info("没有需要处理的过期注销用户");
                return;
            }
            
            log.info("找到 {} 个过期注销用户，开始处理", expiredUsers.size());
            
            for (UserDO userDO : expiredUsers) {
                try {
                    deleteUserData(userDO.getId());
                    log.info("成功删除用户数据: userId={}", userDO.getId());
                } catch (Exception e) {
                    log.error("删除用户数据失败: userId={}", userDO.getId(), e);
                }
            }
            
            log.info("用户注销定时任务执行完成，处理 {} 个用户", expiredUsers.size());
            
        } catch (Exception e) {
            log.error("用户注销定时任务执行失败", e);
        }
    }
    
    /**
     * 删除用户的所有数据
     */
    private void deleteUserData(Long userId) {
        log.info("开始删除用户数据: userId={}", userId);
        
        // 1. 查询用户的所有宠物
        var pets = petRepository.findByUserId(userId);
        
        for (var pet : pets) {
            Long petId = pet.getId().getValue();
            
            // 2. 软删除宠物的各类记录
            feedRecordRepository.deleteByPetId(petId);
            waterRecordRepository.deleteByPetId(petId);
            excretionRecordRepository.deleteByPetId(petId);
            weightRecordRepository.deleteByPetId(petId);
            eventRecordRepository.deleteByPetId(petId);
            reminderRepository.deleteByPetId(petId);
            photoRepository.deleteByPetId(petId);
            
            // 3. 软删除宠物
            petRepository.deleteById(pet.getId());
            
            log.info("删除宠物数据成功: petId={}", petId);
        }
        
        // 4. 软删除用户
        userRepository.delete(userId);
        
        log.info("用户数据删除完成: userId={}", userId);
    }
}
