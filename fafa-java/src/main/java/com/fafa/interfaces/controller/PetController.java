package com.fafa.interfaces.controller;

import com.fafa.application.dto.pet.PetInfoDTO;
import com.fafa.application.dto.pet.PetPhotoSlot;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物接口控制器
 * <p>
 * 头像与三视图已合并为一套按位序上传的照片（位1=头像、位2=正面照、位3=侧面照）：
 * 创建宠物必须携带三张照片；更新照片走统一的替换接口，可传新文件或引用现有照片调整位序。
 *
 * @author FaFa Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "宠物管理", description = "宠物档案 CRUD、三视图照片（位1头像）、排序")
public class PetController {

    private final PetApplicationService petApplicationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "创建宠物", description = "普通用户限2只，会员不限。必须按位序上传三张照片（photos[0]=头像、photos[1]=正面照、photos[2]=侧面照）")
    public Result<PetDetailResponse> createPet(
        @Valid @ModelAttribute CreatePetRequest request
    ) {
        RegisterPetCommand command = new RegisterPetCommand(
            request.getName(),
            request.getType(),
            request.getBreed(),
            request.getGender(),
            request.getBirthDate(),
            request.getIsNeutered(),
            request.getAdoptDate(),
            request.getWeight(),
            request.getPhotos()
        );

        PetInfoDTO dto = petApplicationService.registerPet(command);
        return Result.ok(convertToResponse(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新宠物信息", description = "更新宠物基本资料（照片请使用统一的替换三视图接口）")
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

    @PutMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "替换三视图照片", description = "按位序替换照片（位1=头像、位2=正面照、位3=侧面照），三张缺一不可。每个槽位二选一：传新文件（avatarFile/frontFile/sideFile），或传现有照片 URL 调整位序（avatarUrl/frontUrl/sideUrl）。被替换的旧照片将从 OSS 删除，并触发重新向量化")
    public Result<PetDetailResponse> replacePetPhotos(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @ModelAttribute ReplacePetPhotosRequest request
    ) {
        PetInfoDTO dto = petApplicationService.replacePetPhotos(
            id,
            new PetPhotoSlot(request.getAvatarFile(), request.getAvatarUrl()),
            new PetPhotoSlot(request.getFrontFile(), request.getFrontUrl()),
            new PetPhotoSlot(request.getSideFile(), request.getSideUrl())
        );
        return Result.ok(convertToResponse(dto));
    }

    @PostMapping("/{id}/photos/revectorize")
    @Operation(summary = "重新触发三视图向量化", description = "向量化失败或任务丢失时的补偿入口：将向量化状态重置为待处理并重新派发任务。照片不齐全或已完成向量化时会被拒绝")
    public Result<PetDetailResponse> revectorizePhotos(
        @Parameter(description = "宠物ID") @PathVariable Long id
    ) {
        PetInfoDTO dto = petApplicationService.revectorizePetPhotos(id);
        return Result.ok(convertToResponse(dto));
    }

    @PutMapping("/{id}/sort")
    @Operation(summary = "调整宠物排序", description = "调整宠物展示顺序")
    public Result<Void> adjustSortOrder(
        @Parameter(description = "宠物ID") @PathVariable Long id,
        @Parameter(description = "展示排序值，数值越小越靠前", required = true, example = "1")
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
        response.setPhotos(dto.photos());
        response.setVectorizationStatus(dto.vectorizationStatus());
        response.setSortOrder(dto.sortOrder());
        return response;
    }
}
