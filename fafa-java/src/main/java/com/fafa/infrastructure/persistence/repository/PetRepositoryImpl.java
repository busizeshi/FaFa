package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.persistence.converter.PetConverter;
import com.fafa.infrastructure.persistence.dataobject.PetDO;
import com.fafa.infrastructure.persistence.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 宠物仓储实现（Infrastructure 层）
 * 
 * 职责：
 * 1. 实现 Domain 层定义的 PetRepository 接口
 * 2. 负责领域对象和数据对象之间的转换
 * 3. 与数据库交互
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PetRepositoryImpl implements PetRepository {

    private final PetMapper petMapper;

    @Override
    public Optional<Pet> findById(PetId id) {
        log.debug("查询宠物: id={}", id.getValue());
        
        PetDO petDO = petMapper.selectById(id.getValue());
        return Optional.ofNullable(PetConverter.toDomain(petDO));
    }

    @Override
    public List<Pet> findByUserId(Long userId) {
        log.debug("查询用户的宠物列表: userId={}", userId);
        
        LambdaQueryWrapper<PetDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PetDO::getUserId, userId)
                .orderByAsc(PetDO::getSortOrder, PetDO::getCreatedAt);
        
        List<PetDO> petDOList = petMapper.selectList(queryWrapper);
        
        return petDOList.stream()
                .map(PetConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Pet save(Pet pet) {
        PetDO petDO = PetConverter.toDO(pet);
        
        if (petDO.getId() == null) {
            // 新增
            log.info("新增宠物: name={}, userId={}", pet.getName(), pet.getUserId());
            petMapper.insert(petDO);
            pet.setId(new PetId(petDO.getId()));
        } else {
            // 更新
            log.info("更新宠物: id={}, name={}", petDO.getId(), pet.getName());
            petMapper.updateById(petDO);
        }
        
        return pet;
    }

    @Override
    public void deleteById(PetId id) {
        log.info("删除宠物: id={}", id.getValue());
        petMapper.deleteById(id.getValue());
    }

    @Override
    public boolean existsById(PetId id) {
        return petMapper.selectById(id.getValue()) != null;
    }

    @Override
    public int countByUserId(Long userId) {
        return petMapper.countByUserId(userId);
    }
}
