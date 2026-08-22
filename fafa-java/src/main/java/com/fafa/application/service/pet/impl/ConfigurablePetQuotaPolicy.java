package com.fafa.application.service.pet.impl;

import com.fafa.application.service.pet.PetQuotaPolicy;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 配置化宠物数量限额策略
 *
 * 普通用户：配置值（默认2只）
 * 会员用户：配置值（默认999只）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurablePetQuotaPolicy implements PetQuotaPolicy {

    private final FaFaProperties properties;
    private final UserRepository userRepository;

    @Override
    public boolean canCreatePet(Long userId, int currentCount) {
        int maxPets = getMaxPets(userId);
        return currentCount < maxPets;
    }

    @Override
    public int getMaxPets(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.isVip()) {
            return properties.getPet().getLimit().getVip();
        } else {
            return properties.getPet().getLimit().getNormal();
        }
    }
}
