package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.photo.PhotoId;
import com.fafa.infrastructure.persistence.dataobject.PhotoDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * 照片转换器
 *
 * @author FaFa
 * @since 2026-08-18
 */
public class PhotoConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * DO -> 领域模型
     */
    public static Photo toDomain(PhotoDO photoDO) {
        if (photoDO == null) {
            return null;
        }

        return Photo.builder()
                .photoId(PhotoId.of(photoDO.getId()))
                .petId(photoDO.getPetId())
                .userId(photoDO.getUserId())
                .url(photoDO.getUrl())
                .thumbnailUrl(photoDO.getThumbnailUrl())
                .originalUrl(photoDO.getOriginalUrl())
                .takenAt(photoDO.getTakenAt())
                .uploadAt(photoDO.getUploadAt())
                .description(photoDO.getDescription())
                .tags(parseJsonArray(photoDO.getTags()))
                .aiTags(parseJsonArray(photoDO.getAiTags()))
                .aiDescription(photoDO.getAiDescription())
                .embeddingId(photoDO.getEmbeddingId())
                .width(photoDO.getWidth())
                .height(photoDO.getHeight())
                .fileSize(photoDO.getFileSize())
                .isCover(photoDO.getIsCover() != null && photoDO.getIsCover() == 1)
                .createdAt(photoDO.getCreatedAt())
                .updatedAt(photoDO.getUpdatedAt())
                .build();
    }

    /**
     * 领域模型 -> DO
     */
    public static PhotoDO toDO(Photo photo) {
        if (photo == null) {
            return null;
        }

        PhotoDO photoDO = new PhotoDO();
        if (photo.getPhotoId() != null && photo.getPhotoId().getValue() != null) {
            photoDO.setId(photo.getPhotoId().getValue());
        }
        photoDO.setPetId(photo.getPetId());
        photoDO.setUserId(photo.getUserId());
        photoDO.setUrl(photo.getUrl());
        photoDO.setThumbnailUrl(photo.getThumbnailUrl());
        photoDO.setOriginalUrl(photo.getOriginalUrl());
        photoDO.setTakenAt(photo.getTakenAt());
        photoDO.setUploadAt(photo.getUploadAt());
        photoDO.setDescription(photo.getDescription());
        photoDO.setTags(toJsonString(photo.getTags()));
        photoDO.setAiTags(toJsonString(photo.getAiTags()));
        photoDO.setAiDescription(photo.getAiDescription());
        photoDO.setEmbeddingId(photo.getEmbeddingId());
        photoDO.setWidth(photo.getWidth());
        photoDO.setHeight(photo.getHeight());
        photoDO.setFileSize(photo.getFileSize());
        photoDO.setIsCover(photo.getIsCover() != null && photo.getIsCover() ? 1 : 0);
        photoDO.setCreatedAt(photo.getCreatedAt());
        photoDO.setUpdatedAt(photo.getUpdatedAt());

        return photoDO;
    }

    /**
     * 解析 JSON 数组字符串
     */
    private static List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 列表转 JSON 字符串
     */
    private static String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }
}
