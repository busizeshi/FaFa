package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.event.CreateEventCommand;
import com.fafa.application.dto.event.EventRecordDTO;
import com.fafa.application.dto.event.UpdateEventCommand;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.event.EventRecord;
import com.fafa.domain.model.event.EventType;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.domain.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 事件记录应用服务
 *
 * Phase 3 仅提供范围查询（完成闭环验证 + 时间轴预览）；
 * 手动事件仅允许创建者编辑/删除；提醒完成生成的事件只读。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventRecordApplicationService {

    private static final int MAX_RANGE_DAYS = 366;

    private final EventRecordRepository eventRecordRepository;
    private final PetApplicationService petApplicationService;
    private final UserTagRepository userTagRepository;

    /**
     * 宠物事件范围查询（日期升序，范围 ≤366 天）
     */
    public List<EventRecordDTO> listByPetAndRange(Long petId, LocalDate startDate, LocalDate endDate) {
        petApplicationService.checkPetOwnership(petId);
        validateRange(startDate, endDate);

        return eventRecordRepository.findByPetIdAndDateRange(petId, startDate, endDate).stream()
                .map(this::toDTO)
                .toList();
    }

    /** 创建手动成长档案事件，并刷新标签热度字典。 */
    @org.springframework.transaction.annotation.Transactional
    public EventRecordDTO createManual(CreateEventCommand command) {
        Pet pet = petApplicationService.checkPetOwnership(command.petId());
        EventRecord record;
        try {
            record = EventRecord.createManual(
                    pet.getId(), pet.getUserId(), parseEventType(command.eventType()), command.title(),
                    command.content(), command.eventDate(), command.materialUrls(), command.tags(),
                    java.time.LocalDateTime.now(), LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, e.getMessage());
        }
        EventRecord saved = eventRecordRepository.save(record);
        touchTags(saved.getUserId(), saved.getTags());
        log.info("手动事件创建成功: eventId={}, petId={}, userId={}", saved.getId(), saved.getPetId(), saved.getUserId());
        return toDTO(saved);
    }

    /** 编辑手动事件；提醒生成的事件不可修改。 */
    @org.springframework.transaction.annotation.Transactional
    public EventRecordDTO updateManual(Long id, UpdateEventCommand command) {
        EventRecord record = findOwned(id);
        ensureManual(record);
        try {
            record.updateManual(parseEventType(command.eventType()), command.title(), command.content(),
                    command.eventDate(), command.materialUrls(), command.tags(), java.time.LocalDateTime.now(),
                    LocalDate.now());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, e.getMessage());
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.EVENT_RECORD_NOT_MODIFIABLE, e.getMessage());
        }
        EventRecord saved = eventRecordRepository.save(record);
        touchTags(saved.getUserId(), saved.getTags());
        log.info("手动事件更新成功: eventId={}, userId={}", id, saved.getUserId());
        return toDTO(saved);
    }

    /** 删除手动事件；提醒生成的事件不可删除。 */
    @org.springframework.transaction.annotation.Transactional
    public void deleteManual(Long id) {
        EventRecord record = findOwned(id);
        ensureManual(record);
        try {
            record.deleteManual();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.EVENT_RECORD_NOT_MODIFIABLE, e.getMessage());
        }
        eventRecordRepository.deleteById(id);
        log.info("手动事件删除成功: eventId={}, userId={}", id, record.getUserId());
    }

    private EventRecord findOwned(Long id) {
        EventRecord record = eventRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_RECORD_NOT_FOUND));
        if (!record.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ErrorCode.EVENT_RECORD_NOT_FOUND);
        }
        return record;
    }

    private void ensureManual(EventRecord record) {
        if (!record.isManual()) {
            throw new BusinessException(ErrorCode.EVENT_RECORD_NOT_MODIFIABLE);
        }
    }

    private EventType parseEventType(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "事件类型不能为空");
        }
        try {
            return EventType.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "事件类型取值非法: " + code);
        }
    }

    private void touchTags(Long userId, List<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            userTagRepository.upsertAndTouch(userId, tags, null, java.time.LocalDateTime.now());
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "起止日期不能为空");
        }
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询范围不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private EventRecordDTO toDTO(EventRecord r) {
        return new EventRecordDTO(
                r.getId(),
                r.getPetId(),
                r.getSource().getCode(),
                r.getEventType().getCode(),
                r.getTitle(),
                r.getContent(),
                r.getEventDate(),
                r.getMaterialUrls(),
                r.getTags(),
                r.getCreatedAt()
        );
    }
}
