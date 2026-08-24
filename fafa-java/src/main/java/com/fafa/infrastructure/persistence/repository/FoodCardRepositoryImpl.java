package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.food.FoodStatus;
import com.fafa.domain.model.food.FoodType;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.infrastructure.persistence.mapper.FoodCardMapper;
import com.fafa.infrastructure.persistence.po.FoodCardPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 食品管理卡仓储实现。 */
@Repository
@RequiredArgsConstructor
public class FoodCardRepositoryImpl implements FoodCardRepository {

    private final FoodCardMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<FoodCard> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public Optional<FoodCard> findByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<FoodCardPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodCardPO::getId, id).eq(FoodCardPO::getUserId, userId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public Optional<FoodCard> findBySourceRecognitionTaskId(Long taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<FoodCardPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodCardPO::getSourceRecognitionTaskId, taskId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public List<FoodCard> findByUserIdAndPetId(Long userId, Long petId, String status) {
        LambdaQueryWrapper<FoodCardPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodCardPO::getUserId, userId).eq(FoodCardPO::getPetId, petId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(FoodCardPO::getStatus, status);
        }
        wrapper.orderByDesc(FoodCardPO::getUpdatedAt).orderByDesc(FoodCardPO::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public FoodCard save(FoodCard foodCard) {
        FoodCardPO po = toPO(foodCard);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private FoodCard toDomain(FoodCardPO po) {
        if (po == null) {
            return null;
        }
        return FoodCard.restore(po.getId(), po.getUserId(), po.getPetId(), po.getName(), po.getBrand(),
                FoodType.fromCode(po.getFoodType()), po.getSpecification(), po.getPackageWeight(),
                po.getPackageWeightUnit(), po.getPrice(), po.getCurrency(), po.getOpenedAt(),
                po.getExpiresAt(), po.getNutritionBasis(), po.getProteinPercent(), po.getFatPercent(),
                po.getFiberPercent(), po.getMoisturePercent(), po.getAshPercent(), po.getCalciumPercent(),
                po.getPhosphorusPercent(), po.getEnergyKcal(), po.getEnergyUnit(), readList(po.getProteinSources()),
                readList(po.getIngredients()), readList(po.getAdditiveFlags()), po.getLifeStageLabel(),
                po.getSourceMediaId(), po.getSourceRecognitionTaskId(), FoodStatus.fromCode(po.getStatus()),
                po.getNotes(), Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }

    private FoodCardPO toPO(FoodCard foodCard) {
        FoodCardPO po = new FoodCardPO();
        po.setId(foodCard.getId());
        po.setUserId(foodCard.getUserId());
        po.setPetId(foodCard.getPetId());
        po.setName(foodCard.getName());
        po.setBrand(foodCard.getBrand());
        po.setFoodType(foodCard.getFoodType().getCode());
        po.setSpecification(foodCard.getSpecification());
        po.setPackageWeight(foodCard.getPackageWeight());
        po.setPackageWeightUnit(foodCard.getPackageWeightUnit());
        po.setPrice(foodCard.getPrice());
        po.setCurrency(foodCard.getCurrency());
        po.setOpenedAt(foodCard.getOpenedAt());
        po.setExpiresAt(foodCard.getExpiresAt());
        po.setNutritionBasis(foodCard.getNutritionBasis());
        po.setProteinPercent(foodCard.getProteinPercent());
        po.setFatPercent(foodCard.getFatPercent());
        po.setFiberPercent(foodCard.getFiberPercent());
        po.setMoisturePercent(foodCard.getMoisturePercent());
        po.setAshPercent(foodCard.getAshPercent());
        po.setCalciumPercent(foodCard.getCalciumPercent());
        po.setPhosphorusPercent(foodCard.getPhosphorusPercent());
        po.setEnergyKcal(foodCard.getEnergyKcal());
        po.setEnergyUnit(foodCard.getEnergyUnit());
        po.setProteinSources(writeList(foodCard.getProteinSources()));
        po.setIngredients(writeList(foodCard.getIngredients()));
        po.setAdditiveFlags(writeList(foodCard.getAdditiveFlags()));
        po.setLifeStageLabel(foodCard.getLifeStageLabel());
        po.setSourceMediaId(foodCard.getSourceMediaId());
        po.setSourceRecognitionTaskId(foodCard.getSourceRecognitionTaskId());
        po.setStatus(foodCard.getStatus().getCode());
        po.setNotes(foodCard.getNotes());
        po.setIsDeleted(foodCard.isDeleted());
        return po;
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("食品卡 JSON 字段无效", ex);
        }
    }

    private String writeList(List<String> values) {
        try {
            return values == null || values.isEmpty() ? null : objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new IllegalStateException("食品卡 JSON 字段序列化失败", ex);
        }
    }
}
