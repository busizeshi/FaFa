package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.MediaAssetPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 媒体素材Mapper
 *
 * @author FaFa Team
 * @since 4.0
 */
@Mapper
public interface MediaAssetMapper extends BaseMapper<MediaAssetPO> {
}
