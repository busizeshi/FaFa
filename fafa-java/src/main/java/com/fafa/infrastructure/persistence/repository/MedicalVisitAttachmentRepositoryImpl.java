package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.medical.MedicalVisitAttachment;
import com.fafa.domain.repository.MedicalVisitAttachmentRepository;
import com.fafa.infrastructure.persistence.mapper.MedicalVisitAttachmentMapper;
import com.fafa.infrastructure.persistence.po.MedicalVisitAttachmentPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MedicalVisitAttachmentRepositoryImpl implements MedicalVisitAttachmentRepository {

    private final MedicalVisitAttachmentMapper mapper;

    @Override
    public MedicalVisitAttachment save(MedicalVisitAttachment attachment) {
        MedicalVisitAttachmentPO po = new MedicalVisitAttachmentPO();
        po.setId(attachment.getId());
        po.setUserId(attachment.getUserId());
        po.setVisitId(attachment.getVisitId());
        po.setMediaId(attachment.getMediaId());
        po.setAttachmentType(attachment.getAttachmentType());
        po.setSortOrder(attachment.getSortOrder());
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public List<MedicalVisitAttachment> findByVisitId(Long visitId) {
        LambdaQueryWrapper<MedicalVisitAttachmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalVisitAttachmentPO::getVisitId, visitId)
                .orderByAsc(MedicalVisitAttachmentPO::getSortOrder)
                .orderByAsc(MedicalVisitAttachmentPO::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByVisitIdAndMediaId(Long visitId, Long mediaId) {
        LambdaQueryWrapper<MedicalVisitAttachmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalVisitAttachmentPO::getVisitId, visitId)
                .eq(MedicalVisitAttachmentPO::getMediaId, mediaId);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public void deleteByVisitIdAndMediaId(Long visitId, Long mediaId) {
        LambdaQueryWrapper<MedicalVisitAttachmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalVisitAttachmentPO::getVisitId, visitId)
                .eq(MedicalVisitAttachmentPO::getMediaId, mediaId);
        mapper.delete(wrapper);
    }

    private MedicalVisitAttachment toDomain(MedicalVisitAttachmentPO po) {
        return MedicalVisitAttachment.restore(po.getId(), po.getUserId(), po.getVisitId(),
                po.getMediaId(), po.getAttachmentType(), po.getSortOrder(), po.getCreatedAt());
    }
}
