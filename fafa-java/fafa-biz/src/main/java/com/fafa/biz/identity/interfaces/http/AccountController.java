package com.fafa.biz.identity.interfaces.http;

import com.fafa.biz.identity.application.*;
import com.fafa.biz.identity.domain.IdentityModels.LegalDocument;
import com.fafa.common.api.R;
import com.fafa.biz.pet.application.PetService;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneOffset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class AccountController {
    public record LegalView(String type, String version, String title, String body,
                            String contentHash, Instant effectiveAt) {
        static LegalView from(LegalDocument document) {
            return new LegalView(document.type(), document.version(), document.title(), document.body(),
                    document.contentHash(), document.effectiveAt().toInstant(ZoneOffset.UTC));
        }
    }
    public record Bootstrap(boolean authenticated, AccountSessions.UserView user,
            List<PetService.PetView> pets, String currentPetId, String nextRoute, Map<String, Boolean> features) { }
    private final AccountSessions sessions;
    private final LegalDocuments legal;
    private final PetService pets;

    public AccountController(AccountSessions sessions, LegalDocuments legal, PetService pets) {
        this.sessions = sessions; this.legal = legal; this.pets = pets;
    }
    @GetMapping("/bootstrap")
    @Operation(operationId = "getBootstrap")
    public R<Bootstrap> bootstrap() {
        var user = sessions.hasToken() ? sessions.me() : null;
        var selection = user == null ? new PetService.Selection(List.of(), null) : pets.selection();
        return R.ok(new Bootstrap(user != null, user, selection.pets(), selection.currentPetId(),
                user == null ? "WELCOME" : selection.currentPetId() == null ? "NO_PET" : "HOME",
                Map.of("ai", false, "community", false, "petManagement", true)));
    }
    @GetMapping("/legal-documents")
    @Operation(operationId = "listLegalDocuments")
    public R<List<LegalView>> legalDocuments(@RequestParam(required = false) String type,
                                            @RequestParam(required = false) String version) {
        return R.ok(legal.read(type, version).stream().map(LegalView::from).toList());
    }
    @GetMapping("/me")
    @Operation(operationId = "getMyProfile")
    @SecurityRequirement(name = "bearerAuth")
    public R<AccountSessions.UserView> me() { return R.ok(sessions.me()); }
    @GetMapping("/me/sessions")
    @Operation(operationId = "listMySessions")
    @SecurityRequirement(name = "bearerAuth")
    public R<List<SessionTokens.Device>> currentDevice() { return R.ok(List.of(sessions.device())); }
}
