package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.pet.Gender;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.persistence.mapper.PetMapper;
import com.fafa.infrastructure.persistence.po.PetPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 宠物仓储实现
 *
 * @author FaFa Team
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class PetRepositoryImpl implements PetRepository {

    private final PetMapper petMapper;

    @Override
    public Pet save(Pet pet) {
        PetPO po = toPetPO(pet);
        
        if (po.getId() == null) {
            petMapper.insert(po);
            po = petMapper.selectById(po.getId());
        } else {
            petMapper.updateById(po);
        }
        
        return toDomain(po);
    }

    @Override
    public Optional<Pet> findById(Long petId) {
        PetPO po = petMapper.selectById(petId);
        return Optional.ofNullable(toDomain(po));
    }

    @Override
    public List<Pet> findByUserId(Long userId) {
        LambdaQueryWrapper<PetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetPO::getUserId, userId)
               .orderByAsc(PetPO::getSortOrder);
        
        return petMapper.selectList(wrapper).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public int countByUserId(Long userId) {
        LambdaQueryWrapper<PetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetPO::getUserId, userId);
        Long count = petMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    @Override
    public boolean existsByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<PetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetPO::getId, id)
               .eq(PetPO::getUserId, userId);
        return petMapper.selectCount(wrapper) > 0;
    }

    @Override
    public void delete(Long id) {
        petMapper.deleteById(id);
    }

    @Override
    public int getMaxSortOrder(Long userId) {
        LambdaQueryWrapper<PetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PetPO::getUserId, userId)
               .orderByDesc(PetPO::getSortOrder)
               .last("LIMIT 1");
        
        PetPO pet = petMapper.selectOne(wrapper);
        return pet != null && pet.getSortOrder() != null ? pet.getSortOrder() : 0;
    }

    /**
     * 持久化对象转领域对象
     */
    private Pet toDomain(PetPO po) {
        if (po == null) {
            return null;
        }

        return Pet.restore(
            po.getId(),
            po.getUserId(),
            po.getName(),
            PetType.valueOf(po.getType().toUpperCase()),
            po.getBreed(),
            Gender.valueOf(po.getGender().toUpperCase()),
            po.getBirthDate(),
            po.getIsNeutered() != null && po.getIsNeutered(),
            po.getAdoptDate(),
            po.getWeight(),
            po.getAvatarUrl(),
            po.getFrontPhotoUrl(),
            po.getSidePhotoUrl(),
            po.getSortOrder(),
            po.getIsDeleted() != null && po.getIsDeleted()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private PetPO toPetPO(Pet pet) {
        if (pet == null) {
            return null;
        }

        PetPO po = new PetPO();
        po.setId(pet.getId());
        po.setUserId(pet.getUserId());
        po.setName(pet.getName());
        po.setType(pet.getType().name().toLowerCase());
        po.setBreed(pet.getBreed());
        po.setGender(pet.getGender().name().toLowerCase());
        po.setBirthDate(pet.getBirthDate());
        po.setIsNeutered(pet.isNeutered());
        po.setAdoptDate(pet.getAdoptDate());
        po.setWeight(pet.getWeight());
        po.setAvatarUrl(pet.getAvatarUrl());
        po.setFrontPhotoUrl(pet.getFrontPhotoUrl());
        po.setSidePhotoUrl(pet.getSidePhotoUrl());
        po.setSortOrder(pet.getSortOrder());
        po.setIsDeleted(pet.isDeleted());

        return po;
    }
}
