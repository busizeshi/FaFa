package com.fafa.application.dto.photo;

import lombok.Data;

/**
 * 照片搜索请求
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class SearchPhotoRequest {

    private Long petId;
    
    private String query;
    
    private Integer limit = 20;
}
