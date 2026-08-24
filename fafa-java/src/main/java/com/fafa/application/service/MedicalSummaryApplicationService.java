package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.medical.MedicalSummaryAgentRequest;
import com.fafa.application.dto.medical.MedicalSummaryCommand;
import com.fafa.application.dto.medical.MedicalSummaryDTO;
import com.fafa.application.port.MedicalSummaryGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 医疗摘要请求校验和外部 AI 编排协调服务。 */
@Service
@RequiredArgsConstructor
public class MedicalSummaryApplicationService {

    private final PetApplicationService petApplicationService;
    private final MedicalSummaryGateway medicalSummaryGateway;
    private final FaFaProperties properties;

    public MedicalSummaryDTO generate(MedicalSummaryCommand command) {
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.checkPetOwnership(command.petId());
        validateRange(command.startDate(), command.endDate());
        return medicalSummaryGateway.generate(new MedicalSummaryAgentRequest(userId, command.petId(),
                command.startDate(), command.endDate(), command.includeMedia(), traceId()));
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end) || end.isAfter(LocalDate.now())
                || ChronoUnit.DAYS.between(start, end) > properties.getMedical().getSummaryMaxRangeDays()) {
            throw new BusinessException(ErrorCode.HEALTH_SUMMARY_INVALID,
                    "摘要日期范围无效，且不能超过 " + properties.getMedical().getSummaryMaxRangeDays() + " 天");
        }
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "no-trace" : traceId;
    }
}
