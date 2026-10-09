package com.fafa.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 宠物信息DTO
 * <p>
 * photos 为按位序排列的三张照片（位1=头像、位2=正面照、位3=侧面照）；
 * vectorizationStatus 为三视图向量化状态：pending/processing/completed/failed。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Schema(description = "宠物信息")
public record PetInfoDTO(
    @Schema(description = "宠物ID", example = "1")
    Long id,
    @Schema(description = "所属用户ID", example = "1")
    Long userId,
    @Schema(description = "宠物名称", example = "小白")
    String name,
    @Schema(description = "宠物类型：cat=猫，dog=狗，other=其他", example = "cat")
    String type,
    @Schema(description = "宠物品种", example = "英短")
    String breed,
    @Schema(description = "宠物性别：male=公，female=母，unknown=未知", example = "male")
    String gender,
    @Schema(description = "出生日期", example = "2023-01-15")
    LocalDate birthDate,
    @Schema(description = "是否绝育", example = "false")
    Boolean neutered,
    @Schema(description = "到家日期", example = "2023-03-20")
    LocalDate adoptDate,
    @Schema(description = "当前体重（kg）", example = "4.5")
    BigDecimal weight,
    @Schema(description = "头像URL（即位1照片）")
    String avatarUrl,
    @Schema(description = "正面照URL（即位2照片）")
    String frontPhotoUrl,
    @Schema(description = "侧面照URL（即位3照片）")
    String sidePhotoUrl,
    @Schema(description = "按位序排列的三张照片URL数组：[头像, 正面, 侧面]")
    List<String> photos,
    @Schema(description = "三视图向量化状态：pending/processing/completed/failed", example = "pending")
    String vectorizationStatus,
    @Schema(description = "排序序号", example = "1")
    Integer sortOrder,
    @Schema(description = "三张照片是否齐全", example = "true")
    Boolean hasProfilePhotos
) {
}
