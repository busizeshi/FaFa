package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.PetMemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PetMemberMapper extends BaseMapper<PetMemberPO> {
    @Select("SELECT * FROM pet_member WHERE pet_id=#{petId} AND user_id=#{userId} LIMIT 1 FOR UPDATE")
    PetMemberPO selectForUpdate(@Param("petId") Long petId, @Param("userId") Long userId);

    @Update("UPDATE pet_member SET status='expired', updated_at=#{now} "
            + "WHERE role='caregiver' AND status='active' AND expires_at IS NOT NULL AND expires_at <= #{now}")
    int expireDue(@Param("now") LocalDateTime now);
}
