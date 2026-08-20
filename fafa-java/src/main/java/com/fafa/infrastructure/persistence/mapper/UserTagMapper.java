package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.dataobject.UserTagDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户标签Mapper
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Mapper
public interface UserTagMapper extends BaseMapper<UserTagDO> {

    /**
     * 根据用户ID查询所有标签
     */
    List<UserTagDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 按使用频率查询标签
     */
    List<UserTagDO> selectByUserIdOrderByUsage(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 根据用户ID和标签名查询
     */
    UserTagDO selectByUserIdAndTagName(@Param("userId") Long userId, @Param("tagName") String tagName);

    /**
     * 统计用户标签数量
     */
    int countByUserId(@Param("userId") Long userId);
}
