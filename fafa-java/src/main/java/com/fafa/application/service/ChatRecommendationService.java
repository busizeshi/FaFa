package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.chat.RecommendationDTO;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.DailyStatusRepository;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.domain.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 基于已有数据的确定性推荐问题服务。 */
@Service
@RequiredArgsConstructor
public class ChatRecommendationService {

    private static final int DEFAULT_LIMIT = 6;

    private final PetApplicationService petApplicationService;
    private final DailyStatusRepository dailyStatusRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final EventRecordRepository eventRecordRepository;

    public RecommendationDTO getRecommendations(Long requestedPetId, int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        Pet pet = resolvePet(userId, requestedPetId);
        if (pet == null) {
            return new RecommendationDTO(null, List.of());
        }
        int safeLimit = Math.min(Math.max(limit, 1), DEFAULT_LIMIT);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        List<RecommendationDTO.Item> items = new ArrayList<>();
        items.add(new RecommendationDTO.Item("weight-trend", pet.getName() + "最近体重怎么样？", "trend"));
        if (!weightRecordRepository.findByPetIdAndDateRange(pet.getId(), today.minusDays(30), today).isEmpty()) {
            items.add(new RecommendationDTO.Item("weight-history", pet.getName() + "最近一个月体重有变化吗？", "trend"));
        }
        if (!dailyStatusRepository.findByPetIdAndDateRange(pet.getId(), monthStart, today).isEmpty()) {
            items.add(new RecommendationDTO.Item("refused-food", pet.getName() + "这个月有几天拒食？", "record"));
        }
        if (!eventRecordRepository.findByPetIdAndDateRange(pet.getId(), today.minusDays(30), today).isEmpty()) {
            items.add(new RecommendationDTO.Item("recent-events", pet.getName() + "最近发生了什么？", "memory"));
        }
        items.add(new RecommendationDTO.Item("feeding-history", pet.getName() + "最近吃了多少？", "record"));
        items.add(new RecommendationDTO.Item("media-memory", "帮我找" + pet.getName() + "最近的照片和视频", "memory"));
        return new RecommendationDTO(pet.getId(), items.stream().limit(safeLimit).toList());
    }

    private Pet resolvePet(Long userId, Long requestedPetId) {
        if (requestedPetId != null) {
            return petApplicationService.checkPetOwnership(requestedPetId);
        }
        return petApplicationService.getUserPetDomains(userId).stream().findFirst().orElse(null);
    }
}
