package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.UserTagPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户标签Mapper
 *
 * @author FaFa Team
 * @since 4.0
 */
@Mapper
public interface UserTagMapper extends BaseMapper<UserTagPO> {

    /**
     * 打标联动原子 upsert：不存在则插入（usage_count=1），存在则计数+1 并刷新最后使用时间。
     * 依赖唯一约束 uk_user_tag_name(user_id, tag_name)。
     */
    @Insert("INSERT INTO user_tag (user_id, tag_name, category, usage_count, last_used_at, created_at, updated_at) "
            + "VALUES (#{userId}, #{tagName}, #{category}, 1, #{now}, #{now}, #{now}) "
            + "ON DUPLICATE KEY UPDATE usage_count = usage_count + 1, last_used_at = VALUES(last_used_at), "
            + "updated_at = VALUES(updated_at)")
    int upsertAndTouch(@Param("userId") Long userId, @Param("tagName") String tagName,
                       @Param("category") String category, @Param("now") LocalDateTime now);
}
