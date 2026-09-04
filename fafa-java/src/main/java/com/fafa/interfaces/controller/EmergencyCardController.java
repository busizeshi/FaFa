package com.fafa.interfaces.controller;

import com.fafa.application.dto.emergency.EmergencyCardDTO;
import com.fafa.application.dto.emergency.EmergencyProfileDTO;
import com.fafa.application.service.EmergencyCardApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.emergency.UpdateEmergencyProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pets/{petId}")
@RequiredArgsConstructor
@Tag(name = "应急信息卡", description = "用户确认的应急资料与卡片事实")
public class EmergencyCardController {
    private final EmergencyCardApplicationService emergencyService;

    @GetMapping("/emergency-profile")
    @Operation(summary = "查询应急资料")
    public Result<EmergencyProfileDTO> getProfile(
            @Parameter(description = "宠物ID", required = true, example = "1") @PathVariable Long petId) { return Result.ok(emergencyService.getProfile(petId)); }

    @PutMapping("/emergency-profile")
    @Operation(summary = "更新应急资料")
    public Result<EmergencyProfileDTO> updateProfile(
                                                     @Parameter(description = "宠物ID", required = true, example = "1") @PathVariable Long petId,
                                                     @Valid @RequestBody UpdateEmergencyProfileRequest request) {
        return Result.ok(emergencyService.update(petId, new EmergencyProfileDTO(petId, request.getBloodType(), request.getAllergies(),
                request.getChronicConditions(), request.getEmergencyContactName(), request.getEmergencyContactPhone(),
                request.getPrimaryHospitalName(), request.getPrimaryHospitalPhone(), request.getPrimaryHospitalAddress(),
                request.getNotes(), request.getVersion(), null)));
    }

    @GetMapping("/emergency-card")
    @Operation(summary = "读取应急信息卡")
    public Result<EmergencyCardDTO> card(
            @Parameter(description = "宠物ID", required = true, example = "1") @PathVariable Long petId) { return Result.ok(emergencyService.card(petId)); }
}
