package com.fafa.biz.pet.domain;

import com.fafa.biz.pet.domain.PetModels.*;
import java.time.Instant;
import java.util.List;

/** 宠物持久化端口；写方法由应用事务和当前用户写锁保护。 */
public interface PetRepository {
    Pet find(String id);
    List<Pet> list(String owner, Status status, String after, int limit);
    void insert(Pet pet);
    void update(Pet pet, long expectedVersion);
    Preferences preferences(String owner);
    void savePreferences(String owner, Preferences preferences);
    void clearSelection(String owner, String petId);
    void requestDeletion(String id, Pet pet, Instant deadline);
    Instant deletionDeadline(String petId);
    Status withdrawDeletion(String petId, Instant now);
}
