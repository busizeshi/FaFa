package com.fafa.biz.pet.application;

import com.fafa.biz.pet.domain.PetModels.*;
import com.fafa.biz.pet.domain.PetRepository;
import com.fafa.common.error.BusinessError;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import org.springframework.stereotype.Component;

/** P0 仅提供主人授权；读取和后续记录模块共用此权限入口。 */
@Component
public class PetAccess {
    private final PetRepository pets;
    public PetAccess(PetRepository pets) { this.pets = pets; }
    public Pet owned(String actor, String id) {
        var pet = pets.find(id);
        if (pet == null || !pet.ownerUserId().equals(actor) || pet.status() == Status.DELETED) {
            throw new FafaException(CommonErrorCode.NOT_FOUND);
        }
        return pet;
    }
    public Pet active(String actor, String id) {
        var pet = owned(actor, id);
        if (pet.status() != Status.ACTIVE) throw new FafaException(BusinessError.PET_STATE_CONFLICT);
        return pet;
    }
    public Pet versioned(String actor, String id, long version) {
        var pet = owned(actor, id);
        if (pet.version() != version) throw new FafaException(BusinessError.VERSION_CONFLICT);
        return pet;
    }
}
