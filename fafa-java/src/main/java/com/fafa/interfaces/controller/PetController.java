package com.fafa.interfaces.controller;

import com.fafa.application.dto.pet.PetInfoDTO;
import com.fafa.application.dto.pet.RegisterPetCommand;
import com.fafa.application.dto.pet.UpdatePetProfileCommand;
import com.fafa.application.service.PetApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.pet.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物接口控制器
 *
 * @author FaFa Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "宠物管理", description = "宠物档案 CRUD、头像/三视图上传、排序")
public class PetController {

    private final PetApplicationService petApplicationService;

    @PostMapping
    @Operation(summary = "创建宠物", description = "普通用户限2只，会员不限")
    public Result<PetDetailResponse> createPet(@Valid @RequestBody CreatePetRequest request) {
        RegisterPetCommand command = new RegisterPetCommand(
            request.getName(),
            request.getType(),
            request.getBreed(),
            request.getGender(),
            request.getBirthDate(),
            request.getIsNeutered(),
            request.getAdoptDate()
        );

        PetInfoDTO dto = petApplicationService.registerPet(command);
        return Result.ok(convertToResponse(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新宠物信息", description = "更新宠物基本资料（不含照片）")
    public Result<PetDetailResponse> updatePet(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @Valid @RequestBody UpdatePetRequest request
    ) {
        UpdatePetProfileCommand command = new UpdatePetProfileCommand(
            id,
            request.getName(),
            request.getBreed(),
            request.getGender(),
            request.getBirthDate(),
            request.getIsNeutered(),
            request.getAdoptDate()
        );

        PetInfoDTO dto = petApplicationService.updatePetProfile(command);
        return Result.ok(convertToResponse(dto));
    }

    @GetMapping
    @Operation(summary = "查询宠物列表", description = "查询当前用户的所有宠物（按排序）")
    public Result<List<PetDetailResponse>> listPets() {
        List<PetInfoDTO> dtos = petApplicationService.getUserPets();

        List<PetDetailResponse> responses = dtos.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());

        return Result.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询宠物详情", description = "获取单个宠物的详细信息")
    public Result<PetDetailResponse> getPetDetail(
        @Parameter(description = "宠物ID") @PathVariable Long id
    ) {
        PetInfoDTO dto = petApplicationService.getPetInfo(id);
        return Result.ok(convertToResponse(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除宠物", description = "逻辑删除")
    public Result<Void> deletePet(
        @Parameter(description = "宠物ID") @PathVariable Long id
    ) {
        petApplicationService.deletePet(id);
        return Result.ok();
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传宠物头像", description = "上传头像照片")
    public Result<PetDetailResponse> uploadAvatar(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file
    ) {
        PetInfoDTO dto = petApplicationService.uploadAvatar(id, file);
        return Result.ok(convertToResponse(dto));
    }

    @PostMapping(value = "/{id}/profile-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传三视图照片", description = "上传正面照+侧面照")
    public Result<PetDetailResponse> uploadProfilePhotos(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @Parameter(description = "正面照") @RequestParam("frontPhoto") MultipartFile frontPhoto,
        @Parameter(description = "侧面照") @RequestParam("sidePhoto") MultipartFile sidePhoto
    ) {
        PetInfoDTO dto = petApplicationService.uploadProfilePhotos(id, frontPhoto, sidePhoto);
        return Result.ok(convertToResponse(dto));
    }

    @PutMapping("/{id}/sort")
    @Operation(summary = "调整宠物排序", description = "调整宠物展示顺序")
    public Result<Void> adjustSortOrder(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @RequestParam("sortOrder") Integer sortOrder
    ) {
        petApplicationService.adjustSortOrder(id, sortOrder);
        return Result.ok();
    }

    private PetDetailResponse convertToResponse(PetInfoDTO dto) {
        PetDetailResponse response = new PetDetailResponse();
        response.setId(dto.id());
        response.setUserId(dto.userId());
        response.setName(dto.name());
        response.setType(dto.type());
        response.setBreed(dto.breed());
        response.setGender(dto.gender());
        response.setBirthDate(dto.birthDate());
        response.setIsNeutered(dto.neutered());
        response.setAdoptDate(dto.adoptDate());
        response.setWeight(dto.weight());
        response.setAvatarUrl(dto.avatarUrl());
        response.setFrontPhotoUrl(dto.frontPhotoUrl());
        response.setSidePhotoUrl(dto.sidePhotoUrl());
        response.setSortOrder(dto.sortOrder());
        return response;
    }
}
