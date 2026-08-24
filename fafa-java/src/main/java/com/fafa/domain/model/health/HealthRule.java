package com.fafa.domain.model.health;

import java.util.Optional;

/** 纯规则接口：只读取上下文，不访问数据库或外部服务。 */
public interface HealthRule {

    String code();

    Optional<HealthAlertDraft> evaluate(HealthRuleContext context);
}
