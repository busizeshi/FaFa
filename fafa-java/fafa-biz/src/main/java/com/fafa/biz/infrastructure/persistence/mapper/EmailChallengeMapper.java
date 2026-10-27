package com.fafa.biz.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.biz.infrastructure.persistence.entity.EmailChallengeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮箱验证挑战 Mapper。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Mapper
public interface EmailChallengeMapper extends BaseMapper<EmailChallengeDO> {
}
