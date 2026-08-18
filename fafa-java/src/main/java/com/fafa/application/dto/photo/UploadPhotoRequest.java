package com.fafa.application.dto.photo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 照片上传请求
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class UploadPhotoRequest {

    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime takenAt;

    private String description;
}
