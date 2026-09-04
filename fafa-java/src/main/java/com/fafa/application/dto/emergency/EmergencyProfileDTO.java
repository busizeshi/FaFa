package com.fafa.application.dto.emergency;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "宠物应急资料")
public record EmergencyProfileDTO(
                                  @Schema(description = "宠物ID", example = "1") Long petId,
                                  @Schema(description = "血型", example = "A") String bloodType,
                                  @Schema(description = "过敏史", example = "对鸡肉过敏") String allergies,
                                  @Schema(description = "慢性病或长期病史", example = "慢性肾病") String chronicConditions,
                                  @Schema(description = "紧急联系人姓名", example = "张三") String emergencyContactName,
                                  @Schema(description = "紧急联系人电话", example = "13800138000") String emergencyContactPhone,
                                  @Schema(description = "首选就诊医院名称", example = "爱宠动物医院") String primaryHospitalName,
                                  @Schema(description = "首选就诊医院电话", example = "010-12345678") String primaryHospitalPhone,
                                  @Schema(description = "首选就诊医院地址", example = "北京市朝阳区示例路1号") String primaryHospitalAddress,
                                  @Schema(description = "补充备注") String notes,
                                  @Schema(description = "资料版本号，用于乐观锁更新", example = "1") int version,
                                  @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt) {
}
