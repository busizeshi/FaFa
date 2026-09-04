package com.fafa.application.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "内容草稿正文")
public record ContentDraftContentDTO(
                                     @Schema(description = "内容标题", example = "小白八月成长记录") String title,
                                     @Schema(description = "内容正文") String body,
                                     @Schema(description = "话题标签列表", example = "[\"#宠物日常\", \"#猫咪\"]") List<String> hashtags,
                                     @Schema(description = "正文中素材的展示顺序") List<Long> mediaOrder,
                                     @Schema(description = "内容引用的事实来源编码") List<String> sourceKeys) {
}
