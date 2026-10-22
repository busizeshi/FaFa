package com.fafa.biz.pet.domain;

import com.fafa.common.rpc.AdminBusinessRpc.Breed;
import java.util.List;

/** 品种字典仓储；停用值仍允许历史资料读取。 */
public interface BreedRepository {
    Breed find(String id);
    List<Breed> list(String species, String query, String after, int limit, boolean includeInactive);
    Breed save(String id, String species, String name, String status, Long version);
}
