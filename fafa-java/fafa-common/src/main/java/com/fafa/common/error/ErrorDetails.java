package com.fafa.common.error;

import java.util.List;

/** 稳定的错误数据载荷；不得包含提交的敏感信息或基础设施详情。 */
public record ErrorDetails(List<FieldViolation> fieldErrors) {
    public ErrorDetails {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
