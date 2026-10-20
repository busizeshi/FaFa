package com.fafa.biz.interfaces.http;

import com.fafa.common.api.ApiResponse;
import com.fafa.common.web.RequestIdContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/biz")
public class SystemController {
    @GetMapping("/ping")
    public ApiResponse<String> ping(@RequestHeader(RequestIdContext.HEADER_NAME) String requestId) {
        return ApiResponse.success("fafa-biz is UP", requestId);
    }
}
