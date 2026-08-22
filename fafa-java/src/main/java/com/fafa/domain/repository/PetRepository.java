package com.fafa.domain.repository;

import com.fafa.domain.model.pet.Pet;

import java.util.List;
import java.util.Optional;

/**
 * 宠物仓储接口
 *
 * @author FaFa Team
 * @since 1.0
 */
public interface PetRepository {

    /**
     * 保存宠物（新建或更新）
     * @return 保存后的宠物（新建时会包含生成的ID）
     */
    Pet save(Pet pet);

    /**
     * 根据 ID 查询宠物
     */
    Optional<Pet> findById(Long id);

    /**
     * 查询用户的所有宠物（不含已删除）
     */
    List<Pet> findByUserId(Long userId);

    /**
     * 统计用户的宠物数量（不含已删除）
     */
    int countByUserId(Long userId);

    /**
     * 查询用户某只宠物是否存在且未删除
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * 删除宠物（逻辑删除）
     */
    void delete(Long id);

    /**
     * 获取用户宠物的最大排序号
     */
    int getMaxSortOrder(Long userId);
}
