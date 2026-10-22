package com.fafa.biz.operation;

import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 业务 HTTP 查询共用的分页配置，不依赖任何具体领域。 */
@Validated
@ConfigurationProperties("fafa.api")
public record Paging(@Min(1) int defaultPageSize, @Min(1) int maxPageSize) {
    public Paging {
        if (defaultPageSize > maxPageSize) throw new IllegalArgumentException("默认页大小不能超过上限");
    }
    public int limit(int requested) {
        if (requested < 0 || requested > maxPageSize) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "分页大小无效");
        return requested == 0 ? defaultPageSize : requested;
    }
}
