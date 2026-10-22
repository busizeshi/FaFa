package com.fafa.biz.identity.infrastructure;

import com.fafa.biz.identity.application.IdentityProperties;
import com.fafa.biz.identity.application.VerificationMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpVerificationMailSender implements VerificationMailSender {
    private final JavaMailSender sender;
    private final IdentityProperties policy;
    public SmtpVerificationMailSender(JavaMailSender sender, IdentityProperties policy) {
        this.sender = sender; this.policy = policy;
    }
    public void send(String target, String code, int validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(policy.mailFrom());
        message.setTo(target);
        message.setSubject("FaFa 邮箱验证码");
        message.setText("你的 FaFa 验证码是：" + code + "。有效期 " + validMinutes
                + " 分钟。请勿向他人提供验证码。如非本人操作，请忽略此邮件。");
        sender.send(message);
    }
}
