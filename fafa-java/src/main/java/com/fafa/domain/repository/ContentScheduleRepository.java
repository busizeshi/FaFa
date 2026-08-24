package com.fafa.domain.repository;

import com.fafa.domain.model.content.ContentSchedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentScheduleRepository {
    ContentSchedule save(ContentSchedule schedule);
    Optional<ContentSchedule> findByIdAndUserId(Long id, Long userId);
    Optional<ContentSchedule> findByUserPetType(Long userId, Long petId, String contentType);
    List<ContentSchedule> findByUserId(Long userId, Long petId);
    List<ContentSchedule> findDue(LocalDateTime now, int limit);
    void delete(ContentSchedule schedule);
}
