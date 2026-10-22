package com.fafa.biz.identity.infrastructure;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.fafa.biz.identity.application.SessionTokens;
import com.fafa.biz.identity.domain.IdentityError;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import org.springframework.stereotype.Component;

@Component
public class SaTokenSessions implements SessionTokens {
    private static final String AUTH_VERSION = "authVersion";
    private final StpLogic logic;
    public SaTokenSessions(StpLogic logic) { this.logic = logic; }

    public Grant issue(String userId, long authVersion, Device device) {
        String token = logic.createLoginSession(userId, new SaLoginParameter().setDeviceType("mobile"));
        SaSession session = logic.getTokenSessionByToken(token);
        session.set(AUTH_VERSION, authVersion);
        session.set("deviceId", device.deviceId());
        session.set("deviceName", device.deviceName());
        return new Grant(token, "Bearer", logic.getTokenTimeout(token));
    }
    public String requireUserId() {
        try {
            return logic.getLoginIdAsString();
        } catch (NotLoginException exception) {
            if (NotLoginException.BE_REPLACED.equals(exception.getType())) {
                throw new FafaException(IdentityError.SESSION_REPLACED);
            }
            throw new FafaException(CommonErrorCode.UNAUTHENTICATED);
        }
    }
    public long currentAuthVersion() {
        SaSession session = currentSession();
        Object value = session.get(AUTH_VERSION);
        return value instanceof Number number ? number.longValue() : -1;
    }
    public Device currentDevice() {
        SaSession session = currentSession();
        return new Device((String) session.get("deviceId"), (String) session.get("deviceName"));
    }
    public boolean hasToken() {
        String token = logic.getTokenValue();
        return token != null && !token.isBlank();
    }
    public void logout() { logic.logout(); }
    public void revokeAll(String userId) { logic.logout(userId); }
    private SaSession currentSession() {
        SaSession session = logic.getTokenSession(false);
        if (session == null) throw new FafaException(IdentityError.SESSION_REVOKED);
        return session;
    }
}
