package com.fafa.domain.model.pet;

import lombok.Value;

@Value
public class PetId {
    Long value;

    public static PetId of(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Pet ID must be positive");
        }
        return new PetId(id);
    }
}
