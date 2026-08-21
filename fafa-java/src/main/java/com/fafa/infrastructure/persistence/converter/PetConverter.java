package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.pet.*;
import com.fafa.infrastructure.persistence.dataobject.PetDO;

/**
 * 宠物数据转换器
 * 
 * 职责：领域对象和数据对象之间的转换
 * 
 * @author FaFa Team
 * @since 1.0
 */
public class PetConverter {

    /**
     * DO 转换为领域对象
     */
    public static Pet toDomain(PetDO petDO) {
        if (petDO == null) {
            return null;
        }

        Pet pet = Pet.create(petDO.getUserId(), petDO.getName(), 
                PetSpecies.fromCode(petDO.getSpecies()));
        
        pet.setId(new PetId(petDO.getId()));
        pet.setAvatar(petDO.getAvatar());
        pet.setFrontViewUrl(petDO.getFrontViewUrl());
        pet.setSideViewUrl(petDO.getSideViewUrl());
        pet.setTopViewUrl(petDO.getTopViewUrl());
        pet.setBreed(petDO.getBreed());
        
        if (petDO.getGender() != null) {
            pet.setGender(PetGender.fromCode(petDO.getGender()));
        }
        
        pet.setBirthDate(petDO.getBirthDate());
        pet.setAdoptDate(petDO.getAdoptDate());
        pet.setWeight(petDO.getWeight());
        pet.setIsNeutered(petDO.getIsNeutered());
        pet.setCoatColor(petDO.getCoatColor());
        pet.setRemarks(petDO.getRemarks());
        pet.setStatus(PetStatus.fromCode(petDO.getStatus()));
        pet.setSortOrder(petDO.getSortOrder());
        pet.setCreatedAt(petDO.getCreatedAt());
        pet.setUpdatedAt(petDO.getUpdatedAt());

        return pet;
    }

    /**
     * 领域对象转换为 DO
     */
    public static PetDO toDO(Pet pet) {
        if (pet == null) {
            return null;
        }

        PetDO petDO = new PetDO();
        
        if (pet.getId() != null) {
            petDO.setId(pet.getId().getValue());
        }
        
        petDO.setUserId(pet.getUserId());
        petDO.setName(pet.getName());
        petDO.setAvatar(pet.getAvatar());
        petDO.setFrontViewUrl(pet.getFrontViewUrl());
        petDO.setSideViewUrl(pet.getSideViewUrl());
        petDO.setTopViewUrl(pet.getTopViewUrl());
        petDO.setSpecies(pet.getSpecies().getCode());
        petDO.setBreed(pet.getBreed());
        
        if (pet.getGender() != null) {
            petDO.setGender(pet.getGender().getCode());
        }
        
        petDO.setBirthDate(pet.getBirthDate());
        petDO.setAdoptDate(pet.getAdoptDate());
        petDO.setWeight(pet.getWeight());
        petDO.setIsNeutered(pet.getIsNeutered());
        petDO.setCoatColor(pet.getCoatColor());
        petDO.setRemarks(pet.getRemarks());
        petDO.setStatus(pet.getStatus().getCode());
        petDO.setSortOrder(pet.getSortOrder());
        petDO.setCreatedAt(pet.getCreatedAt());
        petDO.setUpdatedAt(pet.getUpdatedAt());

        return petDO;
    }
}
