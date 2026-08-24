package com.fafa.application.dto.chat;

import java.util.List;

/** AI 助手推荐问题。 */
public record RecommendationDTO(Long petId, List<Item> items) {
    public record Item(String id, String text, String category) {
    }
}
