package com.fafa.admin.interfaces.http;

import com.fafa.admin.security.AdminSessions;
import com.fafa.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.context.annotation.Profile("!admin-init")
@RequestMapping("/v1")
public class AdminAuthController {
    public record Credentials(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,64}") String username,
                              @NotBlank @Size(max = 128) String password) {
        @Override public String toString() { return "AdminCredentials[REDACTED]"; }
    }
    private final AdminSessions sessions;
    public AdminAuthController(AdminSessions sessions) { this.sessions = sessions; }
    @PostMapping("/auth/sessions") @Operation(operationId = "createAdminSession")
    public R<AdminSessions.Login> login(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        return R.ok(sessions.login(body.username(), body.password(), request.getRemoteAddr()));
    }
    @DeleteMapping("/auth/session") @Operation(operationId = "deleteAdminSession")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    public R<Void> logout(@RequestHeader("Authorization") String authorization) { sessions.logout(authorization); return R.ok(); }
    @GetMapping("/me") @Operation(operationId = "getAdminProfile")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    public R<AdminSessions.Principal> me(HttpServletRequest request) { return R.ok(AdminWebConfiguration.current(request)); }
}
