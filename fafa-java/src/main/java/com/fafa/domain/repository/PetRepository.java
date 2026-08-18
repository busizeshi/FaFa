package com.fafa.domain.repository;

import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;

import java.util.List;
import java.util.Optional;

/**
 * 宠物仓储接口（Domain 层定义）
 * 
 * 职责：
 * 1. 定义宠物聚合的持久化接口
 * 2. 由 Infrastructure 层实现
 * 3. 隔离领域层和基础设施层
 * 
 * @author FaFa Team
 * @since 1.0
 */
public interface PetRepository {

    /**
     * 根据 ID 查找宠物
     * 
     * @param id 宠物 ID
     * @return 宠物实体
     */
    Optional<Pet> findById(PetId id);

    /**
     * 根据用户 ID 查找宠物列表
     * 
     * @param userId 用户 ID
     * @return 宠物列表
     */
    List<Pet> findByUserId(Long userId);

    /**
     * 保存宠物
     * 
     * @param pet 宠物实体
     * @return 保存后的宠物实体
     */
    Pet save(Pet pet);

    /**
     * 删除宠物
     * 
     * @param id 宠物 ID
     */
    void deleteById(PetId id);

    /**
     * 检查宠物是否存在
     * 
     * @param id 宠物 ID
     * @return true 存在，false 不存在
     */
    boolean existsById(PetId id);

    /**
     * 统计用户的宠物数量
     * 
     * @param userId 用户 ID
     * @return 宠物数量
     */
    int countByUserId(Long userId);
}
