package com.fafa.application.service;

import com.fafa.application.dto.event.*;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.record.EventRecord;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.domain.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件记录应用服务
 */
@Service
@RequiredArgsConstructor
public class EventRecordApplicationService {
    
    private final EventRecordRepository eventRecordRepository;
    private final PetRepository petRepository;
    
    /**
     * 创建事件记录
     */
    @Transactional
    public Long createEventRecord(Long userId, CreateEventRecordRequest request) {
        // 验证宠物归属
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 创建记录
        EventRecord eventRecord = EventRecord.create(
                request.getPetId(),
                userId,
                request.getEventTime(),
                request.getEventType(),
                request.getTitle()
        );
        eventRecord.setContent(request.getContent());
        eventRecord.setImages(request.getImages());
        eventRecord.setRemarks(request.getRemarks());
        
        EventRecord savedRecord = eventRecordRepository.save(eventRecord);
        
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 查询事件记录列表
     */
    public List<EventRecordResponse> listEventRecords(Long userId, Long petId, String eventType, 
                                                      LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录");
        }
        
        List<EventRecord> records = eventRecordRepository.findByPetId(petId, eventType, startDate, endDate);
        
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询事件记录详情
     */
    public EventRecordResponse getEventRecord(Long userId, Long id) {
        EventRecord record = eventRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该记录");
        }
        
        return convertToResponse(record);
    }
    
    /**
     * 更新事件记录
     */
    @Transactional
    public void updateEventRecord(Long userId, Long id, UpdateEventRecordRequest request) {
        // 验证记录归属
        EventRecord record = eventRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录");
        }
        
        // 更新记录
        record.updateEventInfo(
                request.getEventTime(),
                request.getEventType(),
                request.getTitle(),
                request.getContent(),
                request.getImages(),
                request.getRemarks()
        );
        
        eventRecordRepository.save(record);
    }
    
    /**
     * 删除事件记录
     */
    @Transactional
    public void deleteEventRecord(Long userId, Long id) {
        EventRecord record = eventRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录");
        }
        
        eventRecordRepository.deleteById(id);
    }
    
    /**
     * 转换为响应对象
     */
    private EventRecordResponse convertToResponse(EventRecord record) {
        return EventRecordResponse.builder()
                .id(record.getRecordId().getValue())
                .petId(record.getPetId())
                .eventTime(record.getEventTime())
                .eventType(record.getEventType())
                .title(record.getTitle())
                .content(record.getContent())
                .images(record.getImages())
                .remarks(record.getRemarks())
                .build();
    }
}
