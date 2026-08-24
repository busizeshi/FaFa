package com.fafa.interfaces.controller;

import com.fafa.application.service.PetInvitationApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.collaboration.AcceptInvitationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet-invitations")
@RequiredArgsConstructor
@Tag(name = "邀请接受", description = "登录用户接受宠物空间一次性邀请")
public class PetInvitationController {
    private final PetInvitationApplicationService invitationService;

    @PostMapping("/accept")
    @Operation(summary = "接受宠物空间邀请")
    public Result<?> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        return Result.ok(invitationService.accept(request.getInvitationId(), request.getToken()));
    }
}
