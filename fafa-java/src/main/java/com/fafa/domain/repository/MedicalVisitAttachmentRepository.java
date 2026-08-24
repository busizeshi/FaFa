package com.fafa.domain.repository;

import com.fafa.domain.model.medical.MedicalVisitAttachment;

import java.util.List;

public interface MedicalVisitAttachmentRepository {

    MedicalVisitAttachment save(MedicalVisitAttachment attachment);

    List<MedicalVisitAttachment> findByVisitId(Long visitId);

    boolean existsByVisitIdAndMediaId(Long visitId, Long mediaId);

    void deleteByVisitIdAndMediaId(Long visitId, Long mediaId);
}
