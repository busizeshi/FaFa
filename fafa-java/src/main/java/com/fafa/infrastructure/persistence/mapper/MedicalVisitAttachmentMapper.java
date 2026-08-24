package com.fafa.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafa.infrastructure.persistence.po.MedicalVisitAttachmentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicalVisitAttachmentMapper extends BaseMapper<MedicalVisitAttachmentPO> {
}
