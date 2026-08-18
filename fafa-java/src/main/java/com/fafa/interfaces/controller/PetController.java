package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.PetApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.pet.Pet;
import com.fafa.interfaces.dto.request.CreatePetRequest;
import com.fafa.interfaces.dto.response.PetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
     * 删除宠物
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除宠物", description = "删除指定宠物")
    public Result<Void> deletePet(
            @Parameter(description = "宠物 ID", required = true)
            @PathVariable Long id) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.deletePet(id, userId);
        
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
