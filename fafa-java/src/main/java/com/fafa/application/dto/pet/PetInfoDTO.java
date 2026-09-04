package com.fafa.application.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物信息DTO
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
    @Schema(description = "头像URL")
    String avatarUrl,
    @Schema(description = "正面照URL")
    String frontPhotoUrl,
    @Schema(description = "侧面照URL")
    String sidePhotoUrl,
    @Schema(description = "排序序号", example = "1")
    Integer sortOrder,
    @Schema(description = "是否已上传正面照和侧面照", example = "true")
    Boolean hasProfilePhotos
) {
}
