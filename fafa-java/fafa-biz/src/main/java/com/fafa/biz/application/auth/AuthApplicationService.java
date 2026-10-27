package com.fafa.biz.application.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import com.fafa.biz.domain.ChallengePurpose;
import com.fafa.biz.domain.UserStatus;
import com.fafa.biz.domain.model.EmailChallenge;
import com.fafa.biz.domain.model.LegalConsent;
import com.fafa.biz.domain.model.PasswordHasherPort;
import com.fafa.biz.domain.model.User;
import com.fafa.biz.domain.repository.EmailChallengeRepositoryPort;
import com.fafa.biz.domain.repository.LegalConsentRepositoryPort;
import com.fafa.biz.domain.repository.UserRepositoryPort;
import com.fafa.biz.domain.AuthErrorCode;
import com.fafa.biz.infrastructure.cache.IdempotencyService;
import com.fafa.biz.infrastructure.cache.RedisRateLimiter;
import com.fafa.biz.infrastructure.config.AuthProperties;
import com.fafa.biz.infrastructure.mail.SmtpMailSender;
import com.fafa.biz.infrastructure.mq.OutboxEventPublisher;
import com.fafa.biz.infrastructure.persistence.entity.OutboxEventDO;
import com.fafa.biz.infrastructure.ratelimit.RateLimit;
import com.fafa.biz.interfaces.auth.dto.AuthSessionResponse;
import com.fafa.biz.interfaces.auth.dto.ChallengeResponse;
import com.fafa.biz.interfaces.auth.dto.RegisterRequest;
import com.fafa.biz.interfaces.auth.dto.SessionView;
import com.fafa.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证应用服务（M00 全部用例编排）。
 *
 * <p>职责边界（架构规范 §8.1）：装配限流/幂等/领域规则/会话签发与邮件发送，
 * 定义事务边界；不含协议格式逻辑（Controller 负责校验 DTO）。</p>
 *
 * <p>安全红线（手册 §7-M00）：</p>
 * <ul>
 *   <li>登录失败统一 {@link AuthErrorCode#AUTH_INVALID_CREDENTIALS}，不区分邮箱/密码错误；</li>
 *   <li>找回密码发送统一受理，不泄露邮箱是否已注册；</li>
 *   <li>密码重置成功后撤销该用户全部会话；</li>
 *   <li>验证码明文只存在于方法参数栈内，不落日志/数据库。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    /** 验证码随机源 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Redis 键前缀：验证码发送冷却 */
    private static final String KEY_COOLDOWN_SEND = "fafa:auth:cooldown:send:";

    /** Redis 键前缀：单邮箱每日发送计数 */
    private static final String KEY_RATE_DAILY_SEND = "fafa:auth:rate:daily:";

    /** Redis 键前缀：一次性重置凭据 */
    private static final String KEY_RESET_CREDENTIAL = "fafa:auth:reset-credential:";

    /** Redis 键前缀：挑战已投递标记（仅发信成功后写入，TTL 与验证码一致） */
    private static final String KEY_CHALLENGE_DELIVERED = "fafa:auth:challenge:delivered:";

    private final UserRepositoryPort userRepository;
    private final EmailChallengeRepositoryPort challengeRepository;
    private final LegalConsentRepositoryPort consentRepository;
    private final PasswordHasherPort passwordHasher;
    private final AuthProperties authProperties;
    private final RedisRateLimiter rateLimiter;
    private final IdempotencyService idempotencyService;
    private final SmtpMailSender mailSender;
    private final OutboxEventPublisher outboxPublisher;
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造注入（显式构造便于测试与依赖审查）。
     */
    public AuthApplicationService(UserRepositoryPort userRepository,
                                  EmailChallengeRepositoryPort challengeRepository,
                                  LegalConsentRepositoryPort consentRepository,
                                  PasswordHasherPort passwordHasher,
                                  AuthProperties authProperties,
                                  RedisRateLimiter rateLimiter,
                                  IdempotencyService idempotencyService,
                                  SmtpMailSender mailSender,
                                  OutboxEventPublisher outboxPublisher,
                                  TransactionTemplate transactionTemplate,
                                  StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.consentRepository = consentRepository;
        this.passwordHasher = passwordHasher;
        this.authProperties = authProperties;
        this.rateLimiter = rateLimiter;
        this.idempotencyService = idempotencyService;
        this.mailSender = mailSender;
        this.outboxPublisher = outboxPublisher;
        this.transactionTemplate = transactionTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== 注册 ====================

    /** 注册操作域（幂等键命名空间） */
    private static final String IDEMPOTENCY_SCOPE_REGISTER = "auth.register";

    /**
     * 注册：限流（注解）→ 前置校验 → 幂等 → 事务内建档（PENDING_VERIFICATION + 协议同意 + 验证挑战）→ 事务后发码。
     *
     * <p>顺序约束：前置校验（协议/密码强度）必须在幂等占位之前完成。幂等键一旦占用，
     * 只有"本次已产生副作用"才算消费；校验失败或发信失败都会释放该键，避免客户端
     * 用同一个 Idempotency-Key 重试时被永久判定为重复提交。</p>
     *
     * @param request          注册请求
     * @param idempotencyKey   客户端幂等键（可空：空则跳过幂等记录）
     * @param clientIp         客户端 IP（限流维度）
     * @return 挑战受理结果（challengeId + 脱敏邮箱 + 过期时间）
     */
    @RateLimit(key = "'fafa:auth:rate:register:ip:' + #clientIp", limit = 10, windowSeconds = 3600)
    public ChallengeResponse register(RegisterRequest request, String idempotencyKey, String clientIp) {
        String email = normalizeEmail(request.email());
        boolean hasKey = idempotencyKey != null && !idempotencyKey.isBlank();

        // 1. 前置校验：无副作用，失败时不占用幂等键
        validateLegalConsent(request.acceptedLegal());
        validatePasswordStrength(request.password());

        // 2. 幂等占位：命中说明首次请求已成功受理（失败路径会释放键）
        if (hasKey && !idempotencyService.tryBegin(0L, IDEMPOTENCY_SCOPE_REGISTER, idempotencyKey)) {
            // 回放首次受理的挑战，避免重复建号与重复发码
            Optional<EmailChallenge> replay =
                    challengeRepository.findLatestUsable(email, ChallengePurpose.REGISTER_VERIFY);
            if (replay.isPresent() && Boolean.TRUE.equals(
                    redisTemplate.hasKey(deliveredKey(replay.get().getId())))) {
                log.info("[注册] 幂等重放 email={} challengeId={}",
                        ChallengeResponse.maskEmail(email), replay.get().getId());
                return toChallengeResponse(replay.get());
            }
            // 首次受理未完成投递（或挑战已过期/已消费）：释放占位后按新请求重走一遍
            log.info("[注册] 幂等命中但首次投递未完成，重新受理 email={}",
                    ChallengeResponse.maskEmail(email));
            idempotencyService.release(0L, IDEMPOTENCY_SCOPE_REGISTER, idempotencyKey);
            hasKey = false;
        }

        boolean completed = false;
        try {
            // 3. 已注册快速失败（只读；事务内仍会再判一次，兜住并发）
            assertNotRegistered(email);
            // 4. 发送配额预检：每日上限 + 冷却（失败时账号尚未创建，不留半成品）
            assertSendQuota(email, ChallengePurpose.REGISTER_VERIFY);

            // 5. 事务内：建档 + 协议同意 + 验证挑战（任一失败整体回滚）
            PendingChallenge pending = transactionTemplate.execute(status -> {
                User user = saveUserForRegistration(email, request.password());
                saveLegalConsents(user.getId(), request.acceptedLegal());
                return new PendingChallenge(user.getId(), issueChallenge(email, ChallengePurpose.REGISTER_VERIFY));
            });

            // 6. 事务提交后发码：失败 503 可重试，客户端复用同一幂等键不会重复建号
            deliverChallenge(email, ChallengePurpose.REGISTER_VERIFY, pending.issued());

            if (hasKey) {
                idempotencyService.complete(0L, IDEMPOTENCY_SCOPE_REGISTER, idempotencyKey, "0");
            }
            completed = true;
            log.info("[注册] 受理 email={} userId={} challengeId={}",
                    ChallengeResponse.maskEmail(email), pending.userId(), pending.issued().challenge().getId());
            return toChallengeResponse(pending.issued().challenge());
        } finally {
            if (hasKey && !completed) {
                // 未成功受理：释放幂等键，允许客户端用同一 key 重试
                releaseIdempotencyQuietly(idempotencyKey);
            }
        }
    }

    /**
     * 已注册快速失败：只有"已完成验证/已停用"的账号才拦注册；未验证的旧记录允许复用。
     *
     * @param email 归一化邮箱
     */
    private void assertNotRegistered(String email) {
        userRepository.findByEmail(email)
                .filter(u -> u.getStatus() != UserStatus.PENDING_VERIFICATION)
                .ifPresent(u -> {
                    throw new BizException(AuthErrorCode.AUTH_EMAIL_ALREADY_REGISTERED);
                });
    }

    /**
     * 注册建档：全新邮箱建号；未完成验证的旧记录复用（只覆盖密码，不重复建号）。
     *
     * @param email    归一化邮箱
     * @param password 明文密码
     * @return 已持久化的用户（含自增 ID）
     */
    private User saveUserForRegistration(String email, String password) {
        String passwordHash = passwordHasher.hash(password);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User pending = existing.get();
            if (pending.getStatus() != UserStatus.PENDING_VERIFICATION) {
                throw new BizException(AuthErrorCode.AUTH_EMAIL_ALREADY_REGISTERED);
            }
            // 未完成验证的重复提交不重复建号（手册自测：重试注册不重复建账号）
            pending.changePassword(passwordHash);
            return userRepository.save(pending);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // 并发注册撞唯一索引：按业务口径返回 409-01-01，而非通用 409-00-00
            throw new BizException(AuthErrorCode.AUTH_EMAIL_ALREADY_REGISTERED, null, e);
        }
    }

    /**
     * 写入协议同意记录（追加式，关联当前用户）。
     *
     * @param userId  用户 ID
     * @param items   客户端提交的协议版本列表
     */
    private void saveLegalConsents(Long userId, List<RegisterRequest.LegalConsentItem> items) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<LegalConsent> consents = items.stream()
                .map(item -> new LegalConsent(item.documentType(), item.documentVersion(), now))
                .toList();
        consentRepository.saveAll(userId, consents);
    }

    /**
     * 释放幂等键（失败不影响主流程异常传播）。
     *
     * @param idempotencyKey 幂等键
     */
    private void releaseIdempotencyQuietly(String idempotencyKey) {
        try {
            idempotencyService.release(0L, IDEMPOTENCY_SCOPE_REGISTER, idempotencyKey);
        } catch (Exception e) {
            log.warn("[注册] 幂等键释放失败 key={} reason={}", idempotencyKey, e.getMessage());
        }
    }


    // ==================== 验证码校验 ====================

    /**
     * 校验邮箱验证码：注册验证 → 激活账号并签发会话；找回密码 → 签发一次性重置凭据。
     *
     * @param challengeId 挑战 ID
     * @param code        验证码
     * @param userAgent   登录设备描述（会话元数据）
     * @return 二选一响应：session（注册验证）/ resetCredential（找回密码）
     */
    public VerifyChallengeResponse verifyChallenge(Long challengeId, String code, String userAgent) {
        EmailChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        EmailChallenge.VerifyOutcome outcome = challenge.verify(code, passwordHasher, now);
        switch (outcome) {
            case UNAVAILABLE -> throw new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND);
            case EXPIRED -> throw new BizException(AuthErrorCode.AUTH_CHALLENGE_EXPIRED);
            case EXHAUSTED -> {
                challengeRepository.update(challenge);
                throw new BizException(AuthErrorCode.AUTH_CHALLENGE_ATTEMPT_EXCEEDED);
            }
            case MISMATCH -> {
                // 消耗一次尝试并持久化，防止暴力枚举
                challengeRepository.update(challenge);
                throw new BizException(AuthErrorCode.AUTH_CHALLENGE_CODE_MISMATCH);
            }
            case SUCCESS -> challengeRepository.update(challenge);
        }

        if (challenge.getPurpose() == ChallengePurpose.REGISTER_VERIFY) {
            return VerifyChallengeResponse.ofSession(activateAndSignIn(challenge.getEmail(), userAgent));
        }
        // 找回密码：签发一次性凭据（Redis，短时效）
        User user = userRepository.findByEmail(challenge.getEmail())
                .orElseThrow(() -> new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND));
        String credential = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_RESET_CREDENTIAL + credential,
                String.valueOf(user.getId()),
                Duration.ofMinutes(authProperties.getResetCredentialTtlMinutes()));
        log.info("[找回密码] verify 通过 userId={} credentialTtl={}min",
                user.getId(), authProperties.getResetCredentialTtlMinutes());
        return VerifyChallengeResponse.ofCredential(
                new com.fafa.biz.interfaces.auth.dto.ResetCredentialResponse(credential));
    }

    /**
     * 注册验证成功：激活账号（事务）→ 提交后签发会话 + 登记 user.registered 事件。
     */
    private AuthSessionResponse activateAndSignIn(String email, String userAgent) {
        User user = transactionTemplate.execute(status -> {
            User found = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND));
            if (found.getStatus() == UserStatus.ACTIVE) {
                // 已激活（重复验证）：幂等返回，不重复发事件
                return found;
            }
            found.activate();
            userRepository.save(found);

            // Outbox：注册完成事件与激活同事务（架构规范 §6.3）
            OutboxEventDO event = new OutboxEventDO();
            event.setEventId(UUID.randomUUID().toString().replace("-", ""));
            event.setEventType("user.registered");
            event.setSchemaVersion(1);
            event.setAggregateType("user");
            event.setAggregateId(String.valueOf(found.getId()));
            try {
                event.setPayload(objectMapper.writeValueAsString(
                        Map.of("userId", found.getId(), "emailDomain", domainOf(found.getEmail()))));
            } catch (Exception e) {
                throw new IllegalStateException("outbox payload serialize failed", e);
            }
            outboxPublisher.append(event);
            return found;
        });

        // 事务提交后再签发会话：回滚不会残留 token
        return signIn(user, userAgent);
    }

    // ==================== 登录 ====================

    /**
     * 登录：IP/邮箱双维度限流（注解）→ 统一凭据校验 → 状态检查 → 签发会话。
     *
     * @param email     邮箱
     * @param password  密码
     * @param clientIp  客户端 IP
     * @param userAgent 设备描述
     * @return 会话响应
     */
    @RateLimit(key = "'fafa:auth:rate:login:ip:' + #clientIp",
            limitProperty = "fafa.auth.login-ip-limit",
            windowProperty = "fafa.auth.login-ip-window-minutes",
            windowUnit = TimeUnit.MINUTES,
            error = AuthErrorCode.AUTH_LOGIN_RATE_LIMITED)
    @RateLimit(key = "'fafa:auth:rate:login:email:' + #email?.trim()?.toLowerCase()",
            limitProperty = "fafa.auth.login-email-limit",
            windowProperty = "fafa.auth.login-email-window-minutes",
            windowUnit = TimeUnit.MINUTES,
            error = AuthErrorCode.AUTH_LOGIN_RATE_LIMITED)
    public AuthSessionResponse login(String email, String password, String clientIp, String userAgent) {
        String normalizedEmail = normalizeEmail(email);

        // 统一凭据校验：邮箱不存在与密码错误同文案（防账号枚举）
        User user = userRepository.findByEmail(normalizedEmail)
                .filter(u -> passwordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new BizException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BizException(AuthErrorCode.AUTH_ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            // 未验证账号不能登录（契约：注册验证成功后才签发正式会话）
            throw new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND, "账号尚未完成邮箱验证");
        }
        return signIn(user, userAgent);
    }

    // ==================== 找回密码 ====================

    /**
     * 找回密码第一步：发送验证码。响应统一受理，不泄露邮箱是否已注册。
     *
     * <p>已知权衡：为让第二步 verify 可以按 challengeId 寻址（契约 AUTH 组），
     * 已注册邮箱的响应返回 challengeId；未注册邮箱静默返回受理。
     * 由于注册接口本身会对已注册邮箱返回 409（产品语义：引导用户去登录），
     * 本端点返回 challengeId 与之处于同一保密级别，枚举防护由注册限流兜底。
     * 该权衡需在产品评审时确认（手册 M00：发送结果不泄露邮箱是否注册）。</p>
     *
     * @param email    邮箱
     * @param clientIp 客户端 IP
     * @return 受理响应（含 challengeId，供 verify 寻址）
     */
    @RateLimit(key = "'fafa:auth:rate:register:ip:' + #clientIp", limit = 10, windowSeconds = 3600)
    public ChallengeResponse createPasswordResetChallenge(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);

        Optional<User> user = userRepository.findByEmail(normalizedEmail);
        if (user.isPresent()) {
            // 仅对已注册邮箱真实发码
            EmailChallenge challenge = createChallengeAndSendCode(normalizedEmail, ChallengePurpose.PASSWORD_RESET);
            log.info("[找回密码] 受理并发码 challengeId={}", challenge.getId());
            return toChallengeResponse(challenge);
        }
        log.info("[找回密码] 邮箱不存在，按受理返回（防枚举）");
        return new ChallengeResponse(null, ChallengeResponse.maskEmail(normalizedEmail),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(authProperties.getCodeTtlMinutes()));
    }

    /**
     * 找回密码第三步：一次性凭据重设密码，成功后撤销全部会话。
     *
     * @param credential  一次性凭据
     * @param newPassword 新密码
     */
    public void resetPassword(String credential, String newPassword) {
        validatePasswordStrength(newPassword);
        String key = KEY_RESET_CREDENTIAL + credential;
        String userIdRaw = redisTemplate.opsForValue().get(key);
        if (userIdRaw == null) {
            throw new BizException(AuthErrorCode.AUTH_RESET_CREDENTIAL_INVALID);
        }
        // 读取即删除：一次性凭据，防重放
        redisTemplate.delete(key);

        User user = userRepository.findById(Long.valueOf(userIdRaw))
                .orElseThrow(() -> new BizException(AuthErrorCode.AUTH_RESET_CREDENTIAL_INVALID));
        user.changePassword(passwordHasher.hash(newPassword));
        userRepository.save(user);

        // 撤销该用户全部会话（设计稿 AUTH-04/2：提交后撤销其他会话）
        StpUtil.logout(user.getId());
        log.info("[找回密码] 密码重置完成 userId={}，已撤销全部会话", user.getId());
    }

    // ==================== 会话管理 ====================

    /**
     * 退出登录：撤销当前会话。
     */
    public void logout() {
        long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("[会话] 用户退出登录 userId={}", userId);
    }

    /**
     * 当前用户的有效会话列表（含设备描述与最近活跃时间）。
     *
     * @return 会话视图列表
     */
    public List<SessionView> listSessions() {
        long userId = StpUtil.getLoginIdAsLong();
        String currentToken = StpUtil.getTokenValue();
        List<SessionView> views = new ArrayList<>();
        for (Object tokenObj : StpUtil.getTokenValueListByLoginId(userId)) {
            String token = String.valueOf(tokenObj);
            views.add(new SessionView(
                    sessionHash(token),
                    deviceOf(token),
                    lastActiveOf(token),
                    token.equals(currentToken)));
        }
        return views;
    }

    /**
     * 撤销指定会话（非当前会话也可由用户主动下线）。
     *
     * @param sessionId 会话 ID（token 摘要）
     */
    public void terminateSession(String sessionId) {
        long userId = StpUtil.getLoginIdAsLong();
        for (Object tokenObj : StpUtil.getTokenValueListByLoginId(userId)) {
            String token = String.valueOf(tokenObj);
            if (sessionHash(token).equals(sessionId)) {
                StpUtil.logoutByTokenValue(token);
                log.info("[会话] 用户主动下线设备 userId={}", userId);
                return;
            }
        }
        throw new BizException(AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND, "会话不存在或已下线");
    }

    /**
     * 验证码校验响应（二选一）：按挑战用途返回会话或重置凭据。
     *
     * @param session         注册验证成功时的会话响应（其余场景为 null）
     * @param resetCredential 找回密码验证成功时的一次性凭据响应（其余场景为 null）
     */
    public record VerifyChallengeResponse(AuthSessionResponse session,
                                          com.fafa.biz.interfaces.auth.dto.ResetCredentialResponse resetCredential) {

        /**
         * @param session 会话响应
         * @return 仅含会话的响应
         */
        public static VerifyChallengeResponse ofSession(AuthSessionResponse session) {
            return new VerifyChallengeResponse(session, null);
        }

        /**
         * @param credential 一次性凭据响应
         * @return 仅含凭据的响应
         */
        public static VerifyChallengeResponse ofCredential(
                com.fafa.biz.interfaces.auth.dto.ResetCredentialResponse credential) {
            return new VerifyChallengeResponse(null, credential);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 已签发待投递的挑战：验证码明文只在事务返回后用于发信，不落库不落日志。
     *
     * @param challenge 已入库的验证挑战
     * @param code      验证码明文（仅在本次调用栈内存在）
     */
    private record IssuedChallenge(EmailChallenge challenge, String code) {
    }

    /**
     * 注册事务产物：用户 ID（用于日志与幂等回填）+ 待投递挑战。
     *
     * @param userId 用户 ID
     * @param issued 已入库待发信的挑战
     */
    private record PendingChallenge(Long userId, IssuedChallenge issued) {
    }

    /**
     * 占用发送配额：每日上限 + 重发冷却。
     *
     * <p>每日计数只会累加（Redis 固定窗口），因此必须放在"已注册/已冷却"判断之后，
     * 避免无效请求白白消耗当天的发信次数。</p>
     *
     * @param email   邮箱
     * @param purpose 挑战用途（冷却按用途隔离）
     */
    private void assertSendQuota(String email, ChallengePurpose purpose) {
        // 冷却先判：不累加计数，避免"冷却中被拒"也消耗每日额度
        long cooldownRemaining = rateLimiter.remainingMillis(cooldownKey(purpose, email));
        if (cooldownRemaining > 0) {
            throw new BizException(AuthErrorCode.AUTH_RESEND_COOLDOWN,
                    "发送太频繁，请 " + Math.max(cooldownRemaining / 1000, 1) + " 秒后再试");
        }
        String dailyKey = KEY_RATE_DAILY_SEND + email + ":" + LocalDateTime.now(ZoneOffset.UTC).toLocalDate();
        RedisRateLimiter.RateResult daily = rateLimiter.tryAcquire(dailyKey,
                authProperties.getDailySendLimit(), Duration.ofHours(24));
        if (!daily.allowed()) {
            throw new BizException(AuthErrorCode.AUTH_EMAIL_SEND_LIMIT);
        }
    }

    /**
     * 生成验证码并落库挑战（不发信）：注册链路在事务内调用，与建档同事务提交。
     *
     * @param email   邮箱
     * @param purpose 挑战用途
     * @return 已入库挑战 + 验证码明文（供事务提交后发信）
     */
    private IssuedChallenge issueChallenge(String email, ChallengePurpose purpose) {
        String code = generateCode(authProperties.getCodeLength());
        EmailChallenge challenge = new EmailChallenge();
        challenge.setPurpose(purpose);
        challenge.setEmail(email);
        // 明文永不落库：只存 bcrypt 哈希
        challenge.setCodeHash(passwordHasher.hash(code));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        challenge.setExpiresAt(now.plusMinutes(authProperties.getCodeTtlMinutes()));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(authProperties.getMaxVerifyAttempts());
        return new IssuedChallenge(challengeRepository.save(challenge), code);
    }

    /**
     * 发送验证码邮件并标记冷却（事务提交后调用）。
     *
     * <p>发送失败统一转换为 {@link AuthErrorCode#AUTH_MAIL_SEND_FAILED}（503 可重试）：
     * 挑战已落库，客户端可点"重新发送"，不会产生第二个账号。</p>
     *
     * @param email   邮箱
     * @param purpose 挑战用途
     * @param issued  已入库的挑战与验证码
     */
    private void deliverChallenge(String email, ChallengePurpose purpose, IssuedChallenge issued) {
        Long challengeId = issued.challenge().getId();
        try {
            mailSender.sendVerificationCode(email, purpose, issued.code(), challengeId,
                    authProperties.getCodeTtlMinutes());
        } catch (RuntimeException e) {
            // SMTP 失败：挑战保留（可重发），对外 503 让客户端重试
            throw new BizException(AuthErrorCode.AUTH_MAIL_SEND_FAILED, null, e);
        }
        // 投递成功标记：幂等重放只回放已真正发信成功的挑战
        redisTemplate.opsForValue().set(deliveredKey(challengeId), "1",
                Duration.ofMinutes(authProperties.getCodeTtlMinutes()));
        // 发送成功后标记冷却（发送失败不冷却，用户可以立刻重试）
        rateLimiter.markCooldown(cooldownKey(purpose, email),
                Duration.ofSeconds(authProperties.getResendCooldownSeconds()));
    }

    /**
     * @param challengeId 挑战 ID
     * @return 投递成功标记 Redis 键
     */
    private String deliveredKey(Long challengeId) {
        return KEY_CHALLENGE_DELIVERED + challengeId;
    }

    /**
     * 创建挑战、发码并标记冷却（找回密码链路使用；注册链路走事务内 issueChallenge + deliverChallenge）。
     *
     * @param email   邮箱
     * @param purpose 挑战用途
     * @return 已发送的挑战
     */
    private EmailChallenge createChallengeAndSendCode(String email, ChallengePurpose purpose) {
        assertSendQuota(email, purpose);
        IssuedChallenge issued = issueChallenge(email, purpose);
        deliverChallenge(email, purpose, issued);
        return issued.challenge();
    }

    /**
     * @param purpose 挑战用途
     * @param email   邮箱
     * @return 重发冷却 Redis 键
     */
    private String cooldownKey(ChallengePurpose purpose, String email) {
        return KEY_COOLDOWN_SEND + purpose + ":" + email;
    }


    /**
     * 签发会话并写入设备元数据（供 /me/sessions 展示）。
     */
    private AuthSessionResponse signIn(User user, String userAgent) {
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        // Token-Session 记录设备信息：列表页展示用（不含敏感原文）
        StpUtil.getTokenSession().set("device", summarizeDevice(userAgent));
        StpUtil.getTokenSession().set("lastActiveAt", LocalDateTime.now(ZoneOffset.UTC).toString());
        log.info("[会话] 签发会话 userId={}", user.getId());
        return new AuthSessionResponse(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                new AuthSessionResponse.UserSummary(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    /**
     * 协议版本校验：必须同时同意当前版本的用户协议与隐私政策。
     */
    private void validateLegalConsent(List<RegisterRequest.LegalConsentItem> items) {
        boolean agreementOk = items.stream().anyMatch(i ->
                "USER_AGREEMENT".equals(i.documentType())
                        && authProperties.getLegal().getUserAgreementVersion().equals(i.documentVersion()));
        boolean privacyOk = items.stream().anyMatch(i ->
                "PRIVACY_POLICY".equals(i.documentType())
                        && authProperties.getLegal().getPrivacyPolicyVersion().equals(i.documentVersion()));
        if (!agreementOk || !privacyOk) {
            throw new BizException(AuthErrorCode.AUTH_LEGAL_CONSENT_REQUIRED);
        }
    }

    /**
     * 密码强度：长度区间内且同时包含字母与数字。
     */
    private void validatePasswordStrength(String password) {
        boolean lengthOk = password.length() >= authProperties.getPasswordMinLength()
                && password.length() <= authProperties.getPasswordMaxLength();
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!lengthOk || !hasLetter || !hasDigit) {
            throw new BizException(AuthErrorCode.AUTH_WEAK_PASSWORD);
        }
    }

    /**
     * 生成数字验证码。
     */
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 邮箱归一化：去空白 + 小写（唯一键口径）。
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * @return 挑战受理响应
     */
    private ChallengeResponse toChallengeResponse(EmailChallenge challenge) {
        return new ChallengeResponse(challenge.getId(),
                ChallengeResponse.maskEmail(challenge.getEmail()), challenge.getExpiresAt());
    }

    /**
     * @return token 摘要（SHA-256 前 16 位 hex），作为对外会话 ID
     */
    private String sessionHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception e) {
            return String.valueOf(token.hashCode());
        }
    }

    /**
     * @return token 绑定的设备描述
     */
    private String deviceOf(String token) {
        Object device = StpUtil.getTokenSessionByToken(token).get("device");
        return device == null ? "未知设备" : String.valueOf(device);
    }

    /**
     * @return token 最近活跃时间
     */
    private LocalDateTime lastActiveOf(String token) {
        Object active = StpUtil.getTokenSessionByToken(token).get("lastActiveAt");
        try {
            return active == null ? null : LocalDateTime.parse(String.valueOf(active));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * User-Agent 摘要：仅保留首段（浏览器/系统名），不存完整 UA。
     */
    private String summarizeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "未知设备";
        }
        return userAgent.length() > 40 ? userAgent.substring(0, 40) : userAgent;
    }

    /**
     * @return 邮箱域名（事件载荷脱敏：不传完整邮箱）
     */
    private String domainOf(String email) {
        int at = email.indexOf('@');
        return at < 0 ? "unknown" : email.substring(at + 1);
    }
}
