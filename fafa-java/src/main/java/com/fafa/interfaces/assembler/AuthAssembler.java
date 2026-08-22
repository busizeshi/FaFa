package com.fafa.interfaces.assembler;

import com.fafa.application.dto.LoginResult;
import com.fafa.interfaces.dto.auth.LoginResponse;
import org.springframework.stereotype.Component;

/**
 * 认证模块 DTO 装配器：应用层结果 → 接口层响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Component
public class AuthAssembler {

    public static LoginResponse toResponse(LoginResult result) {
        LoginResponse response = new LoginResponse();
        response.setToken(result.token());
        response.setUserId(result.userId());
        response.setNewUser(result.newUser());
        return response;
    }
}
