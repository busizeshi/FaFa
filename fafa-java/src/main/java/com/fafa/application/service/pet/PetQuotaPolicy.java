package com.fafa.application.service.pet;

/**
 * 宠物数量限额策略接口
 *
 * @author FaFa Team
 * @since 1.0
 */
public interface PetQuotaPolicy {

    /**
     * 判断用户是否可以创建新宠物
     *
     * @param userId       用户ID
     * @param currentCount 当前宠物数量
     * @return true 可以创建，false 已达上限
     */
    boolean canCreatePet(Long userId, int currentCount);

    /**
     * 获取用户的宠物数量上限
     *
     * @param userId 用户ID
     * @return 宠物数量上限
     */
    int getMaxPets(Long userId);
}
