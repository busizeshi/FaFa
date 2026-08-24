package com.fafa.interfaces.dto.emergency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新宠物应急资料请求")
public class UpdateEmergencyProfileRequest {
    @NotNull(message = "资料版本不能为空")
    @Schema(description = "首次保存填0，更新时提交上次返回的版本号", example = "1")
    private Integer version;
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String primaryHospitalName;
    private String primaryHospitalPhone;
    private String primaryHospitalAddress;
    private String notes;
}
