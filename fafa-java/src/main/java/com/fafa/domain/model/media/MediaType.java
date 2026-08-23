package com.fafa.domain.model.media;

import lombok.Getter;

/**
 * 媒体类型
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public enum MediaType {

    PHOTO("photo", "图片"),
    VIDEO("video", "视频");

    private final String code;
    private final String label;

    MediaType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MediaType fromCode(String code) {
        for (MediaType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid media type: " + code);
    }
}
