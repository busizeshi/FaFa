package com.fafa.application.port;

import com.fafa.application.dto.medical.MedicalSummaryAgentRequest;
import com.fafa.application.dto.medical.MedicalSummaryDTO;

/** 医疗摘要外部编排端口。 */
public interface MedicalSummaryGateway {
    MedicalSummaryDTO generate(MedicalSummaryAgentRequest request);
}
