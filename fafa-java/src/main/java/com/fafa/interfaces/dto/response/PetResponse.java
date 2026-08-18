package com.fafa.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 宠物响应
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "宠物信息")
public class PetResponse {

    @Schema(description = "宠物 ID")
    private Long id;

    @Schema(description = "宠物名称")
    private String name;

    @Schema(description = "宠物头像")
    private String avatar;

    @Schema(description = "种类")
    private String species;

    @Schema(description = "种类名称")
    private String speciesName;

    @Schema(description = "品种")
    private String breed;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "性别名称")
    private String genderName;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "到家日期")
    private LocalDate adoptDate;

    @Schema(description = "年龄（月）")
    private Integer ageInMonths;

    @Schema(description = "当前体重")
    private Double weight;

    @Schema(description = "是否绝育")
    private Boolean isNeutered;

    @Schema(description = "毛色")
    private String coatColor;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;
}
