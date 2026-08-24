package com.fafa.application.service;

import com.fafa.application.dto.content.ContentScheduleDTO;
import com.fafa.application.dto.content.CreateContentDraftCommand;
import com.fafa.application.dto.content.UpsertContentScheduleCommand;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.content.ContentSchedule;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.ContentScheduleRepository;
import com.fafa.domain.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentScheduleApplicationService {
    private final ContentScheduleRepository scheduleRepository;
    private final PetRepository petRepository;
    private final ContentDraftApplicationService draftService;

    public ContentScheduleDTO upsert(UpsertContentScheduleCommand command) {
        ownedPet(command.userId(), command.petId());
        validate(command.contentType(), command.intervalDays());
        LocalDateTime next = command.nextRunAt() == null ? LocalDateTime.now().plusDays(command.intervalDays()) : command.nextRunAt();
        ContentSchedule item = scheduleRepository.findByUserPetType(command.userId(), command.petId(), command.contentType()).orElse(null);
        if (item == null) item = ContentSchedule.create(command.userId(), command.petId(), command.contentType(),
                command.topicType() == null ? "monthly_summary" : command.topicType(), command.intervalDays(), next, LocalDateTime.now());
        else item.update(command.intervalDays(), next, LocalDateTime.now());
        return toDTO(scheduleRepository.save(item));
    }

    public List<ContentScheduleDTO> list(Long userId, Long petId) {
        if (petId != null) ownedPet(userId, petId);
        return scheduleRepository.findByUserId(userId, petId).stream().map(this::toDTO).toList();
    }

    public ContentScheduleDTO enable(Long userId, Long id, boolean enabled) {
        ContentSchedule item = owned(userId, id);
        if (enabled) item.enable(LocalDateTime.now()); else item.disable(LocalDateTime.now());
        return toDTO(scheduleRepository.save(item));
    }

    public void delete(Long userId, Long id) {
        ContentSchedule item = owned(userId, id); item.delete(); scheduleRepository.save(item);
    }

    public void runDueSchedules() {
        for (ContentSchedule schedule : scheduleRepository.findDue(LocalDateTime.now(), 10)) {
            try {
                draftService.create(new CreateContentDraftCommand(schedule.getUserId(), schedule.getPetId(),
                        schedule.getContentType(), schedule.getTopicType(), null, "month", LocalDate.now(),
                        List.of(), "schedule-" + schedule.getId() + "-" + schedule.getNextRunAt(), "scheduled"));
                schedule.markRun(LocalDateTime.now()); scheduleRepository.save(schedule);
            } catch (RuntimeException ex) {
                // 保留 next_run_at，下一轮继续尝试；失败原因由草稿或日志记录。
                log.warn("周期内容生成失败，将在下一轮重试: scheduleId={}", schedule.getId(), ex);
            }
        }
    }

    private ContentSchedule owned(Long userId, Long id) {
        return scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_SCHEDULE_NOT_FOUND));
    }
    private Pet ownedPet(Long userId, Long id) {
        return petRepository.findById(id).filter(item -> userId.equals(item.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN));
    }
    private void validate(String type, int interval) {
        if (!List.of("share_note", "xiaohongshu").contains(type) || !List.of(7, 15, 30).contains(interval))
            throw new BusinessException(ErrorCode.CONTENT_SCHEDULE_INVALID);
    }
    private ContentScheduleDTO toDTO(ContentSchedule item) {
        return new ContentScheduleDTO(item.getId(), item.getPetId(), item.getContentType(), item.getTopicType(),
                item.getIntervalDays(), item.getNextRunAt(), item.getLastRunAt(), item.isEnabled(), item.getUpdatedAt());
    }
}
