package com.fafa.admin.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 只在独立初始化进程中启用；已有账号时拒绝重复创建或覆盖。 */
@Component
@Profile("admin-init")
public class AdminInitializer implements ApplicationRunner {
    private final AdminSessions sessions;
    private final String username;
    private final String password;
    public AdminInitializer(AdminSessions sessions, @Value("${FAFA_ADMIN_INIT_USERNAME}") String username,
                            @Value("${FAFA_ADMIN_INIT_PASSWORD}") String password) {
        this.sessions = sessions; this.username = username; this.password = password;
    }
    public void run(ApplicationArguments args) { sessions.bootstrap(username, password); }
}
