package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.PetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 宠物 Mapper
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Mapper
public interface PetMapper extends BaseMapper<PetDO> {

    /**
     * 根据用户 ID 查询宠物列表
     * 
     * @param userId 用户 ID
     * @return 宠物列表
     */
    List<PetDO> selectByUserId(Long userId);

    /**
     * 统计用户的宠物数量
     * 
     * @param userId 用户 ID
     * @return 数量
     */
    int countByUserId(Long userId);
}
