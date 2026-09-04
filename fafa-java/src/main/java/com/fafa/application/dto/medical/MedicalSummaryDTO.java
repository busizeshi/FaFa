package com.fafa.application.dto.medical;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "就诊摘要")
public record MedicalSummaryDTO(
                                @Schema(description = "宠物基础资料") Map<String, Object> pet,
                                @Schema(description = "摘要统计日期范围") Map<String, String> range,
                                @Schema(description = "就诊摘要概览") String overview,
                                @Schema(description = "症状时间线") List<Map<String, Object>> symptomTimeline,
                                @Schema(description = "就医档案摘要") List<Map<String, Object>> medicalVisits,
                                @Schema(description = "用药记录摘要") List<Map<String, Object>> medications,
                                @Schema(description = "需要重点关注的事项") List<String> attentionItems,
                                @Schema(description = "摘要生成警告") List<String> warnings,
                                @Schema(description = "面向用户的纯文本摘要") String plainText,
                                @Schema(description = "医疗信息免责声明") String disclaimer) {
}
