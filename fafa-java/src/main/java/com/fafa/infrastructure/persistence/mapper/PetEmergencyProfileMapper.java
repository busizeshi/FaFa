package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.PetEmergencyProfilePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PetEmergencyProfileMapper extends BaseMapper<PetEmergencyProfilePO> {
    @Select("SELECT * FROM pet_emergency_profile WHERE pet_id=#{petId} LIMIT 1 FOR UPDATE")
    PetEmergencyProfilePO selectForUpdate(@Param("petId") Long petId);
}
