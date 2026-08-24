package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.medical.MedicalVisit;
import com.fafa.domain.repository.MedicalVisitRepository;
import com.fafa.infrastructure.persistence.mapper.MedicalVisitMapper;
import com.fafa.infrastructure.persistence.po.MedicalVisitPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicalVisitRepositoryImpl implements MedicalVisitRepository {

    private final MedicalVisitMapper mapper;

    @Override
    public MedicalVisit save(MedicalVisit visit) {
        MedicalVisitPO po = toPO(visit);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<MedicalVisit> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public List<MedicalVisit> findByPetIdAndDateRange(Long petId, LocalDate start,
                                                      LocalDate end, int offset, int limit) {
        LambdaQueryWrapper<MedicalVisitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalVisitPO::getPetId, petId)
                .ge(MedicalVisitPO::getVisitDate, start)
                .le(MedicalVisitPO::getVisitDate, end)
                .orderByDesc(MedicalVisitPO::getVisitDate)
                .last("LIMIT " + offset + "," + limit);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<MedicalVisitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalVisitPO::getPetId, petId)
                .ge(MedicalVisitPO::getVisitDate, start)
                .le(MedicalVisitPO::getVisitDate, end);
        return mapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private MedicalVisit toDomain(MedicalVisitPO po) {
        if (po == null) {
            return null;
        }
        return MedicalVisit.restore(po.getId(), po.getUserId(), po.getPetId(), po.getVisitDate(),
                po.getHospitalName(), po.getDoctorName(), po.getChiefComplaint(), po.getDiagnosis(),
                po.getPrescription(), po.getTreatment(), po.getTotalCost(), po.getNotes(),
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }

    private MedicalVisitPO toPO(MedicalVisit visit) {
        MedicalVisitPO po = new MedicalVisitPO();
        po.setId(visit.getId());
        po.setUserId(visit.getUserId());
        po.setPetId(visit.getPetId());
        po.setVisitDate(visit.getVisitDate());
        po.setHospitalName(visit.getHospitalName());
        po.setDoctorName(visit.getDoctorName());
        po.setChiefComplaint(visit.getChiefComplaint());
        po.setDiagnosis(visit.getDiagnosis());
        po.setPrescription(visit.getPrescription());
        po.setTreatment(visit.getTreatment());
        po.setTotalCost(visit.getTotalCost());
        po.setNotes(visit.getNotes());
        po.setIsDeleted(visit.isDeleted());
        return po;
    }
}
