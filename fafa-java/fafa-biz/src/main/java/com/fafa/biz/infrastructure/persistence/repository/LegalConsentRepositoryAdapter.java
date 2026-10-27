package com.fafa.biz.infrastructure.persistence.repository;

import com.fafa.biz.domain.model.LegalConsent;
import com.fafa.biz.domain.repository.LegalConsentRepositoryPort;
import com.fafa.biz.infrastructure.persistence.entity.LegalConsentDO;
import com.fafa.biz.infrastructure.persistence.mapper.LegalConsentMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 协议同意记录仓储适配器：值对象 → DO 映射后追加写入。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class LegalConsentRepositoryAdapter implements LegalConsentRepositoryPort {

    private final LegalConsentMapper legalConsentMapper;

    /**
     * @param legalConsentMapper MyBatis-Plus Mapper
     */
    public LegalConsentRepositoryAdapter(LegalConsentMapper legalConsentMapper) {
        this.legalConsentMapper = legalConsentMapper;
    }

    /** {@inheritDoc} */
    @Override
    public void saveAll(Long userId, List<LegalConsent> consents) {
        for (LegalConsent consent : consents) {
            LegalConsentDO consentDO = new LegalConsentDO();
            consentDO.setUserId(userId);
            consentDO.setDocumentType(consent.documentType());
            consentDO.setDocumentVersion(consent.documentVersion());
            consentDO.setConsentedAt(consent.consentedAt());
            legalConsentMapper.insert(consentDO);
        }
    }
}
