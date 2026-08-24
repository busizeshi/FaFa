package com.fafa.domain.repository;

import com.fafa.domain.model.food.FoodCard;

import java.util.List;
import java.util.Optional;

/** 食品管理卡仓储接口。 */
public interface FoodCardRepository {

    Optional<FoodCard> findById(Long id);

    Optional<FoodCard> findByIdAndUserId(Long id, Long userId);

    Optional<FoodCard> findBySourceRecognitionTaskId(Long taskId);

    List<FoodCard> findByUserIdAndPetId(Long userId, Long petId, String status);

    FoodCard save(FoodCard foodCard);

    void deleteById(Long id);
}
