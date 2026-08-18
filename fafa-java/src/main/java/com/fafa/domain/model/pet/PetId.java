package com.fafa.domain.model.pet;

import lombok.Getter;

import java.util.Objects;

/**
 * 宠物 ID 值对象
 * 
 * 值对象特征：
 * 1. 不可变
 * 2. 无副作用
 * 3. 可替换性
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class PetId {

    private final Long value;

    public PetId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("宠物 ID 不能为空或小于等于 0");
        }
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetId petId = (PetId) o;
        return Objects.equals(value, petId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
