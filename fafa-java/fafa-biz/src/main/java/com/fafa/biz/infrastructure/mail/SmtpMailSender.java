package com.fafa.biz.infrastructure.mail;

import com.fafa.biz.domain.ChallengePurpose;
import com.fafa.biz.infrastructure.persistence.entity.EmailSendLogDO;
import com.fafa.biz.infrastructure.persistence.mapper.EmailSendLogMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * SMTP 邮件发送适配器（M00：验证码邮件；M04 通知类邮件复用本组件扩展）。
 *
 * <p>约定（架构规范 §6.7 / §10）：</p>
 * <ul>
 *   <li>发件账号/授权码走环境变量 SMTP_USERNAME / SMTP_PASSWORD，不入 Git 不入日志；</li>
 *   <li>正文只含验证码与有效期提示，不携带诊断信息或长期链接；</li>
 *   <li>发送结果写 email_send_log（审计），验证码明文永不落日志/落库；</li>
 *   <li>dev 环境额外把验证码写入 Redis（fafa:dev:last-code:{email}，与验证码同 TTL），
 *       便于自测取码——生产 profile 不存在该行为。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class SmtpMailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    /** dev 环境验证码回显 Redis 键前缀（仅 dev 激活，见 {@link #echoDevCodeIfDev}） */
    private static final String DEV_CODE_KEY_PREFIX = "fafa:dev:last-code:";

    private final JavaMailSender mailSender;
    private final EmailSendLogMapper emailSendLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final Environment environment;
    private final String fromAddress;
    private final String fromName;

    /**
     * @param mailSender          Spring Boot Mail 自动装配（163 SSL 配置见 application-dev.yml）
     * @param emailSendLogMapper  发送日志 Mapper
     * @param redisTemplate       Redis（dev 回显用）
     * @param environment         环境探测（dev 回显开关）
     * @param fromAddress         发件地址（spring.mail.username）
     * @param fromName            发件显示名
     */
    public SmtpMailSender(JavaMailSender mailSender,
                          EmailSendLogMapper emailSendLogMapper,
                          StringRedisTemplate redisTemplate,
                          Environment environment,
                          @Value("${spring.mail.username}") String fromAddress,
                          @Value("${fafa.mail.from-name:FaFa}") String fromName) {
        this.mailSender = mailSender;
        this.emailSendLogMapper = emailSendLogMapper;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    /**
     * 发送验证码邮件（同步发送；失败抛出由应用层转换为可重试业务错误）。
     *
     * @param toEmail     收件邮箱
     * @param purpose     用途（决定邮件文案）
     * @param code        验证码明文（仅本次调用内存在，不落日志/不返回前端）
     * @param challengeId 关联挑战 ID（审计）
     * @param ttlMinutes  验证码有效期（分钟，正文提示用）
     * @throws MailException SMTP 发送失败
     */
    public void sendVerificationCode(String toEmail, ChallengePurpose purpose,
                                     String code, Long challengeId, int ttlMinutes) {
        String subject = "FaFa 验证码";
        // 文案区分用途：注册验证 / 找回密码（不携带任何账号诊断信息）
        String scene = purpose == ChallengePurpose.REGISTER_VERIFY ? "注册账号" : "找回密码";
        String content = """
                    你正在进行 %s 操作。

                    验证码：%s

                    验证码 %d 分钟内有效。若非本人操作，请忽略本邮件并注意账号安全。

                    —— FaFa 宠物照护
                    """.formatted(scene, code, ttlMinutes);

        boolean success;
        String errorMsg = null;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
            success = true;
            log.info("[邮件] 验证码邮件已发送 to={} purpose={} challengeId={}", toEmail, purpose, challengeId);
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            success = false;
            // 只记录异常类型与摘要，不记录授权码/完整堆栈
            errorMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("[邮件] 验证码邮件发送失败 to={} reason={}", toEmail, errorMsg);
        }

        // 审计：发送结果落库（不记验证码）
        recordSendLog(toEmail, purpose, success, errorMsg, challengeId);

        if (!success) {
            // 发送失败向上传播：应用层转换为 AUTH_MAIL_SEND_FAILED（503 可重试）
            throw new IllegalStateException("SMTP send failed");
        }
        // dev 自测取码（生产不存在）
        echoDevCodeIfDev(toEmail, code, ttlMinutes);
    }

    /**
     * dev profile 下把验证码写入 Redis 供自测取码；生产/测试 profile 直接跳过。
     */
    private void echoDevCodeIfDev(String toEmail, String code, int ttlMinutes) {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equals(profile)) {
                redisTemplate.opsForValue().set(DEV_CODE_KEY_PREFIX + toEmail, code,
                        Duration.ofMinutes(Math.max(ttlMinutes, 1)));
                log.info("[邮件][dev] 验证码已写入 dev 回显键（仅开发环境）");
                return;
            }
        }
    }

    /**
     * 记录发送日志（审计，失败不影响主流程）。
     */
    private void recordSendLog(String toEmail, ChallengePurpose purpose,
                               boolean success, String errorMsg, Long challengeId) {
        try {
            EmailSendLogDO logDO = new EmailSendLogDO();
            logDO.setToEmail(toEmail);
            logDO.setPurpose(purpose.name());
            logDO.setStatus(success ? "SENT" : "FAILED");
            logDO.setErrorMsg(errorMsg);
            logDO.setChallengeId(challengeId);
            emailSendLogMapper.insert(logDO);
        } catch (Exception ex) {
            log.warn("[邮件] 发送日志写入失败 challengeId={}", challengeId, ex);
        }
    }
}
