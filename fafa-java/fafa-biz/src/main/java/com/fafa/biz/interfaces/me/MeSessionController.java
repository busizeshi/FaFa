package com.fafa.biz.interfaces.me;

import com.fafa.biz.application.auth.AuthApplicationService;
import com.fafa.biz.interfaces.auth.dto.SessionView;
import com.fafa.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 我的会话管理接口（契约 SESSIONS 组，M00）。
 *
 * <p>需登录（Sa-Token 拦截器统一校验）；tokenName 为 fafa-token。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "SESSIONS · 会话管理", description = "会话列表与远程下线（M00）")
public class MeSessionController {

    private final AuthApplicationService authService;

    /**
     * @param authService 认证应用服务
     */
    public MeSessionController(AuthApplicationService authService) {
        this.authService = authService;
    }

    /**
     * 当前用户的有效会话列表。
     *
     * @return 会话视图（含设备与最近活跃时间，current 标记本机）
     */
    @GetMapping("/sessions")
    @Operation(summary = "会话列表：多设备在线状态")
    public ApiResponse<List<SessionView>> listSessions() {
        return ApiResponse.ok(authService.listSessions());
    }

    /**
     * 撤销指定会话（远程下线）。
     *
     * @param id 会话 ID（列表返回的 sessionId）
     * @return 成功确认
     */
    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "撤销会话（远程下线）")
    public ApiResponse<Void> terminateSession(@PathVariable("id") String id) {
        authService.terminateSession(id);
        return ApiResponse.ok(null);
    }
}
