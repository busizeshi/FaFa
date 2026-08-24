package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ChatSessionPO;
import org.apache.ibatis.annotations.Mapper;

/** AI 会话 Mapper。 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionPO> {
}
