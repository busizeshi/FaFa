package com.fafa.application.dto.emergency;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "宠物应急信息卡")
public record EmergencyCardDTO(
                               @Schema(description = "宠物摘要") PetSummary pet,
                               @Schema(description = "应急资料摘要") ProfileSummary profile,
                               @Schema(description = "紧急联系人") Contact emergencyContact,
                               @Schema(description = "首选就诊医院") Hospital primaryHospital,
                               @Schema(description = "资料来源状态") String sourceStatus,
                               @Schema(description = "资料最后更新时间") LocalDateTime updatedAt) {
    @Schema(description = "应急信息卡中的宠物摘要")
    public record PetSummary(
                             @Schema(description = "宠物ID", example = "1") Long id,
                             @Schema(description = "宠物名称", example = "小白") String name,
                             @Schema(description = "宠物类型", example = "cat") String type,
                             @Schema(description = "宠物品种", example = "英短") String breed,
                             @Schema(description = "宠物性别", example = "male") String gender,
                             @Schema(description = "出生日期", example = "2023-01-15") LocalDate birthDate,
                             @Schema(description = "当前体重（kg）", example = "4.5") BigDecimal weight) { }
    @Schema(description = "应急资料摘要")
    public record ProfileSummary(
                                 @Schema(description = "血型", example = "A") String bloodType,
                                 @Schema(description = "过敏史") String allergies,
                                 @Schema(description = "慢性病或长期病史") String chronicConditions,
                                 @Schema(description = "补充备注") String notes) { }
    @Schema(description = "紧急联系人")
    public record Contact(
                         @Schema(description = "联系人姓名", example = "张三") String name,
                         @Schema(description = "联系人电话", example = "13800138000") String phone) { }
    @Schema(description = "首选就诊医院")
    public record Hospital(
                          @Schema(description = "医院名称", example = "爱宠动物医院") String name,
                          @Schema(description = "医院电话", example = "010-12345678") String phone,
                          @Schema(description = "医院地址", example = "北京市朝阳区示例路1号") String address) { }
}
