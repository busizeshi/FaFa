package com.fafa.application.dto.content;

import java.util.List;

public record ContentDraftContentDTO(String title, String body, List<String> hashtags,
                                     List<Long> mediaOrder, List<String> sourceKeys) {
}
