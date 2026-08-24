package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.ContentDraftPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContentDraftMapper extends BaseMapper<ContentDraftPO> {
}
