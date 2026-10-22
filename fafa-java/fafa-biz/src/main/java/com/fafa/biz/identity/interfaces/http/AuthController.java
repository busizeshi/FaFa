package com.fafa.biz.identity.interfaces.http;

import com.fafa.biz.identity.application.*;
import com.fafa.common.api.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    public record RegistrationView(AccountSessions.UserView user, SessionTokens.Grant session, String nextAction) { }
    private final EmailChallenges challenges;
    private final RegisterAccount registrations;
    private final AccountSessions sessions;
    private final ResetPassword passwords;
    private final ClientAddressResolver addresses;
    private final AuthRateLimiter limiter;

    public AuthController(EmailChallenges challenges, RegisterAccount registrations, AccountSessions sessions,
                          ResetPassword passwords, ClientAddressResolver addresses, AuthRateLimiter limiter) {
        this.challenges = challenges; this.registrations = registrations; this.sessions = sessions;
        this.passwords = passwords; this.addresses = addresses; this.limiter = limiter;
    }
    @PostMapping("/email-challenges")
    @Operation(operationId = "createEmailChallenge")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public R<EmailChallenges.Receipt> challenge(@Valid @RequestBody IdentityRequests.EmailChallenge body,
                                               HttpServletRequest request) {
        return R.ok(challenges.create(body.purpose(), body.email(), addresses.resolve(request)));
    }
    @PostMapping("/registrations")
    @Operation(operationId = "registerAccount")
    @ResponseStatus(HttpStatus.CREATED)
    public R<RegistrationView> register(@Valid @RequestBody IdentityRequests.Registration body,
                                        HttpServletRequest request) {
        limiter.verification(addresses.resolve(request));
        String id = registrations.register(body.email(), body.password(), body.confirmedPassword(),
                body.challengeId(), body.code(), body.legalVersions());
        try {
            var result = sessions.afterRegistration(id, body.device().toModel());
            return R.ok(new RegistrationView(result.user(), result.session(), "NO_PET"));
        } catch (DataAccessException unavailable) {
            return R.ok(new RegistrationView(sessions.user(id), null, "LOGIN"));
        }
    }
    @PostMapping("/sessions")
    @Operation(operationId = "login")
    public R<AccountSessions.LoginView> login(@Valid @RequestBody IdentityRequests.Login body,
                                             HttpServletRequest request) {
        return R.ok(sessions.login(body.email(), body.password(), body.device().toModel(), addresses.resolve(request)));
    }
    @DeleteMapping("/session")
    @Operation(operationId = "logout")
    @SecurityRequirement(name = "bearerAuth")
    public R<Void> logout() { sessions.logout(); return R.ok(); }

    @PostMapping("/password-reset-tickets")
    @Operation(operationId = "verifyPasswordReset")
    public R<ResetPassword.TicketView> verify(@Valid @RequestBody IdentityRequests.VerifyReset body,
                                             HttpServletRequest request) {
        limiter.verification(addresses.resolve(request));
        return R.ok(passwords.verify(body.challengeId(), body.code()));
    }
    @PostMapping("/password-resets")
    @Operation(operationId = "resetPassword")
    public R<Void> reset(@Valid @RequestBody IdentityRequests.PasswordReset body, HttpServletRequest request) {
        limiter.verification(addresses.resolve(request));
        passwords.reset(body.resetTicket(), body.password(), body.confirmedPassword());
        return R.ok();
    }
}
