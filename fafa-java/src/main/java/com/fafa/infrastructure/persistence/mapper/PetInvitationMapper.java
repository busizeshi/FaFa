package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.PetInvitationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PetInvitationMapper extends BaseMapper<PetInvitationPO> {
    @Select("SELECT * FROM pet_invitation WHERE id=#{id} AND token_hash=#{tokenHash} LIMIT 1 FOR UPDATE")
    PetInvitationPO selectForUpdate(@Param("id") Long id, @Param("tokenHash") String tokenHash);
}
