package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.PetApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.pet.Pet;
import com.fafa.infrastructure.oss.OssService;
import com.fafa.interfaces.dto.request.CreatePetRequest;
import com.fafa.interfaces.dto.request.SortPetsRequest;
import com.fafa.interfaces.dto.request.UpdatePetRequest;
import com.fafa.interfaces.dto.response.PetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 宠物管理 Controller
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
@Tag(name = "宠物管理", description = "宠物档案相关接口")
public class PetController {

    private final PetApplicationService petApplicationService;
    private final OssService ossService;

    /**
     * 创建宠物
     */
    @PostMapping
    @Operation(summary = "创建宠物", description = "为当前用户创建一只新宠物")
    public Result<Long> createPet(@Valid @RequestBody CreatePetRequest request) {
        // 从 Sa-Token 获取当前登录用户 ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        Long petId = petApplicationService.createPet(
                userId, 
                request.getName(), 
                request.getSpecies()
        );
        
        return Result.success(petId);
    }

    /**
     * 查询宠物详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询宠物详情", description = "根据 ID 查询宠物信息")
    public Result<PetResponse> getPet(
            @Parameter(description = "宠物 ID", required = true)
            @PathVariable Long id) {
        
        Pet pet = petApplicationService.getPet(id);
        PetResponse response = convertToResponse(pet);
        
        return Result.success(response);
    }

    /**
     * 查询我的宠物列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询我的宠物列表", description = "查询当前用户的所有宠物")
    public Result<List<PetResponse>> listMyPets() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<Pet> pets = petApplicationService.listUserPets(userId);
        List<PetResponse> responses = pets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return Result.success(responses);
    }

    /**
     * 更新宠物
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新宠物", description = "更新指定宠物信息")
    public Result<Void> updatePet(
            @Parameter(description = "宠物 ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdatePetRequest request) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.updatePet(id, userId, request);
        
        return Result.success();
    }

    /**
     * 删除宠物
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除宠物", description = "删除指定宠物（软删除）")
    public Result<Void> deletePet(
            @Parameter(description = "宠物 ID", required = true)
            @PathVariable Long id) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.deletePet(id, userId);
        
        return Result.success();
    }
    
    /**
     * 上传宠物头像
     */
    @PostMapping("/{id}/avatar")
    @Operation(summary = "上传宠物头像", description = "上传宠物头像图片")
    public Result<String> uploadAvatar(
            @Parameter(description = "宠物 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") MultipartFile file) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 上传文件到 OSS
        String avatarUrl = ossService.uploadFile(file, "pets/avatars");
        
        // 更新宠物头像
        petApplicationService.updatePetAvatar(id, userId, avatarUrl);
        
        return Result.success(avatarUrl);
    }
    
    /**
     * 宠物排序
     */
    @PutMapping("/sort")
    @Operation(summary = "宠物排序", description = "调整宠物显示顺序")
    public Result<Void> sortPets(@Valid @RequestBody SortPetsRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.sortPets(userId, request.getPetIds());
        return Result.success();
    }

    /**
     * 转换为响应对象
     */
    private PetResponse convertToResponse(Pet pet) {
        PetResponse response = new PetResponse();
        response.setId(pet.getId().getValue());
        response.setName(pet.getName());
        response.setAvatar(pet.getAvatar());
        response.setSpecies(pet.getSpecies().getCode());
        response.setSpeciesName(pet.getSpecies().getDescription());
        response.setBreed(pet.getBreed());
        
        if (pet.getGender() != null) {
            response.setGender(pet.getGender().getCode());
            response.setGenderName(pet.getGender().getDescription());
        }
        
        response.setBirthDate(pet.getBirthDate());
        response.setAdoptDate(pet.getAdoptDate());
        response.setAgeInMonths(pet.getAgeInMonths());
        response.setWeight(pet.getWeight());
        response.setIsNeutered(pet.getIsNeutered());
        response.setCoatColor(pet.getCoatColor());
        response.setRemarks(pet.getRemarks());
        response.setStatus(pet.getStatus().getCode());
        response.setStatusName(pet.getStatus().getDescription());
        
        return response;
    }
}
