package com.fafa.biz.domain.repository;

import com.fafa.biz.domain.model.LegalConsent;

import java.util.List;

/**
 * 协议同意记录仓储端口（追加式写入，不提供更新/删除）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public interface LegalConsentRepositoryPort {

    /**
     * 批量保存同意记录（注册时一次保存用户协议 + 隐私政策）。
     *
     * @param userId   用户 ID
     * @param consents 同意记录值对象
     */
    void saveAll(Long userId, List<LegalConsent> consents);
}
