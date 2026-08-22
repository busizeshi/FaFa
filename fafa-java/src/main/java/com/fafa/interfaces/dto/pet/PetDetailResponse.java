package com.fafa.interfaces.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物详情响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "宠物详情响应")
public class PetDetailResponse {

    @Schema(description = "宠物ID", example = "1234567890")
    private Long id;

    @Schema(description = "所属用户ID", example = "9876543210")
    private Long userId;

    @Schema(description = "宠物名称", example = "小白")
    private String name;

    @Schema(description = "宠物类型：cat/dog/other", example = "cat")
    private String type;

    @Schema(description = "品种", example = "英短")
    private String breed;

    @Schema(description = "性别：male/female/unknown", example = "male")
    private String gender;

    @Schema(description = "出生日期", example = "2023-01-15")
    private LocalDate birthDate;

    @Schema(description = "是否绝育", example = "false")
    private Boolean isNeutered;

    @Schema(description = "到家日期", example = "2023-03-20")
    private LocalDate adoptDate;

    @Schema(description = "当前体重（kg）", example = "4.5")
    private BigDecimal weight;

    @Schema(description = "头像URL", example = "https://oss.fafa.com/avatar/123.jpg")
    private String avatarUrl;

    @Schema(description = "正面照URL", example = "https://oss.fafa.com/profile/front_123.jpg")
    private String frontPhotoUrl;

    @Schema(description = "侧面照URL", example = "https://oss.fafa.com/profile/side_123.jpg")
    private String sidePhotoUrl;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "创建时间", example = "2023-03-20T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2023-03-21T15:45:00")
    private LocalDateTime updatedAt;
}
