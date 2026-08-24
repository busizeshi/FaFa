package com.fafa.interfaces.controller;

import com.fafa.application.dto.collaboration.InvitationDTO;
import com.fafa.application.service.PetInvitationApplicationService;
import com.fafa.application.service.PetMemberApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.collaboration.CreateInvitationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "家庭协同", description = "宠物空间成员、邀请和权限")
public class PetCollaborationController {
    private final PetMemberApplicationService memberService;
    private final PetInvitationApplicationService invitationService;

    @GetMapping("/{petId}/access")
    @Operation(summary = "查询当前用户访问权限")
    public Result<?> access(@PathVariable Long petId) { return Result.ok(memberService.access(petId)); }

    @GetMapping("/{petId}/members")
    @Operation(summary = "查询宠物空间成员")
    public Result<?> members(@PathVariable Long petId) { return Result.ok(memberService.list(petId)); }

    @PostMapping("/{petId}/invitations")
    @Operation(summary = "创建一次性邀请")
    public Result<InvitationDTO> createInvitation(@PathVariable Long petId,
                                                   @Valid @RequestBody CreateInvitationRequest request) {
        return Result.ok(invitationService.create(petId, request.getRole(), request.getExpiresInDays()));
    }

    @DeleteMapping("/{petId}/members/{memberId}")
    @Operation(summary = "撤销成员")
    public Result<Void> revokeMember(@PathVariable Long petId, @PathVariable Long memberId) {
        memberService.revoke(petId, memberId);
        return Result.ok();
    }

    @DeleteMapping("/{petId}/invitations/{invitationId}")
    @Operation(summary = "撤销邀请")
    public Result<Void> revokeInvitation(@PathVariable Long petId, @PathVariable Long invitationId) {
        invitationService.revokeInvitation(petId, invitationId);
        return Result.ok();
    }
}
