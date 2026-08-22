package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.PetPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物Mapper
 *
 * @author FaFa Team
 * @since 1.0
 */
@Mapper
public interface PetMapper extends BaseMapper<PetPO> {
}
