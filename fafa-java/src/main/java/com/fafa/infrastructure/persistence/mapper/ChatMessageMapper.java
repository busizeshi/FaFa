package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;

/** AI 消息 Mapper。 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessagePO> {
}
