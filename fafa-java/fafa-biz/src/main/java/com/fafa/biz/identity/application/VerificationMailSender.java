package com.fafa.biz.identity.application;

public interface VerificationMailSender {
    void send(String target, String code, int validMinutes);
}
