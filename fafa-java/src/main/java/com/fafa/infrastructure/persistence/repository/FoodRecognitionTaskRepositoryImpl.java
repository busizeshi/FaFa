package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.food.FoodRecognitionStatus;
import com.fafa.domain.model.food.FoodRecognitionTask;
import com.fafa.domain.repository.FoodRecognitionTaskRepository;
import com.fafa.infrastructure.persistence.mapper.FoodRecognitionTaskMapper;
import com.fafa.infrastructure.persistence.po.FoodRecognitionTaskPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 食品识别任务仓储实现。 */
@Repository
@RequiredArgsConstructor
public class FoodRecognitionTaskRepositoryImpl implements FoodRecognitionTaskRepository {

    private final FoodRecognitionTaskMapper mapper;

    @Override
    public Optional<FoodRecognitionTask> findByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<FoodRecognitionTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodRecognitionTaskPO::getId, id).eq(FoodRecognitionTaskPO::getUserId, userId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public Optional<FoodRecognitionTask> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey) {
        LambdaQueryWrapper<FoodRecognitionTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodRecognitionTaskPO::getUserId, userId)
                .eq(FoodRecognitionTaskPO::getIdempotencyKey, idempotencyKey);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public FoodRecognitionTask save(FoodRecognitionTask task) {
        FoodRecognitionTaskPO po = toPO(task);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    private FoodRecognitionTask toDomain(FoodRecognitionTaskPO po) {
        if (po == null) {
            return null;
        }
        return FoodRecognitionTask.restore(po.getId(), po.getUserId(), po.getPetId(), po.getMediaId(),
                po.getIdempotencyKey(), FoodRecognitionStatus.fromCode(po.getStatus()), po.getModelName(),
                po.getPromptVersion(), po.getDraftJson(), po.getRawResponseJson(), po.getErrorCode(),
                po.getErrorMessage(), po.getFoodCardId(), po.getQuotaMonth(), po.getStartedAt(),
                po.getCompletedAt(), po.getExpiredAt(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private FoodRecognitionTaskPO toPO(FoodRecognitionTask task) {
        FoodRecognitionTaskPO po = new FoodRecognitionTaskPO();
        po.setId(task.getId());
        po.setUserId(task.getUserId());
        po.setPetId(task.getPetId());
        po.setMediaId(task.getMediaId());
        po.setIdempotencyKey(task.getIdempotencyKey());
        po.setStatus(task.getStatus().getCode());
        po.setModelName(task.getModelName());
        po.setPromptVersion(task.getPromptVersion());
        po.setDraftJson(task.getDraftJson());
        po.setRawResponseJson(task.getRawResponseJson());
        po.setErrorCode(task.getErrorCode());
        po.setErrorMessage(task.getErrorMessage());
        po.setFoodCardId(task.getFoodCardId());
        po.setQuotaMonth(task.getQuotaMonth());
        po.setStartedAt(task.getStartedAt());
        po.setCompletedAt(task.getCompletedAt());
        po.setExpiredAt(task.getExpiredAt());
        return po;
    }
}
