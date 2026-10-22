package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.ChallengePurpose;

public interface AuthRateLimiter {
    void challenge(String email, String address, ChallengePurpose purpose);
    void login(String email, String address);
    void verification(String address);
}
