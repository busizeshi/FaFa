package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.AccessAuditLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AccessAuditLogMapper extends BaseMapper<AccessAuditLogPO> {
    @Select("SELECT * FROM access_audit_log WHERE pet_id=#{petId} "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<AccessAuditLogPO> selectByPetId(@Param("petId") Long petId, @Param("offset") int offset,
                                         @Param("limit") int limit);
}
