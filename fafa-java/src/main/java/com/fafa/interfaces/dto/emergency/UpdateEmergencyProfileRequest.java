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
    @Schema(description = "血型", example = "A")
    private String bloodType;
    @Schema(description = "过敏史", example = "对鸡肉过敏")
    private String allergies;
    @Schema(description = "慢性病或长期病史", example = "慢性肾病")
    private String chronicConditions;
    @Schema(description = "紧急联系人姓名", example = "张三")
    private String emergencyContactName;
    @Schema(description = "紧急联系人电话", example = "13800138000")
    private String emergencyContactPhone;
    @Schema(description = "首选就诊医院名称", example = "爱宠动物医院")
    private String primaryHospitalName;
    @Schema(description = "首选就诊医院电话", example = "010-12345678")
    private String primaryHospitalPhone;
    @Schema(description = "首选就诊医院地址", example = "北京市朝阳区示例路1号")
    private String primaryHospitalAddress;
    @Schema(description = "补充备注", example = "请携带既往检查报告")
    private String notes;
}
