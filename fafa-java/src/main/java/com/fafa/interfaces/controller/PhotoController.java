package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.photo.*;
import com.fafa.application.service.PhotoApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.photo.Photo;
import com.fafa.infrastructure.client.PythonAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 照片控制器
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Slf4j
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    @Resource
    private PhotoApplicationService photoApplicationService;

    @Resource
    private PythonAiClient pythonAiClient;

    /**
     * 上传照片
     */
    @PostMapping("/upload")
    public Result<PhotoResponse> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("petId") Long petId,
            @RequestParam(value = "takenAt", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime takenAt,
            @RequestParam(value = "description", required = false) String description) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 文件校验
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }
        
        // 文件大小校验（限制 10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("图片大小不能超过 10MB");
        }
        
        Long photoId = photoApplicationService.uploadPhoto(userId, petId, file, takenAt, description);
        Photo photo = photoApplicationService.getPhoto(userId, photoId);
        
        return Result.success(toResponse(photo));
    }

    /**
     * 查询照片列表
     */
    @GetMapping
    public Result<List<PhotoResponse>> listPhotos(
            @RequestParam("petId") Long petId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<Photo> photos = photoApplicationService.listPhotos(userId, petId, page, size);
        List<PhotoResponse> responses = photos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return Result.success(responses);
    }

    /**
     * 根据日期范围查询照片
     */
    @GetMapping("/date-range")
    public Result<List<PhotoResponse>> listPhotosByDateRange(
            @RequestParam("petId") Long petId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<Photo> photos = photoApplicationService.listPhotosByDateRange(userId, petId, startDate, endDate);
        List<PhotoResponse> responses = photos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return Result.success(responses);
    }

    /**
     * 查询照片详情
     */
    @GetMapping("/{id}")
    public Result<PhotoResponse> getPhoto(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        Photo photo = photoApplicationService.getPhoto(userId, id);
        return Result.success(toResponse(photo));
    }

    /**
     * 更新照片描述
     */
    @PutMapping("/{id}/description")
    public Result<Void> updatePhotoDescription(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdatePhotoDescriptionRequest request) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        photoApplicationService.updatePhotoDescription(userId, id, request.getDescription());
        return Result.success();
    }

    /**
     * 设置为封面照片
     */
    @PutMapping("/{id}/cover")
    public Result<Void> setCoverPhoto(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        photoApplicationService.setCoverPhoto(userId, id);
        return Result.success();
    }

    /**
     * 删除照片
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePhoto(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        photoApplicationService.deletePhoto(userId, id);
        return Result.success();
    }

    /**
     * 统计照片数量
     */
    @GetMapping("/count")
    public Result<Integer> countPhotos(@RequestParam("petId") Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        int count = photoApplicationService.countPhotos(userId, petId);
        return Result.success(count);
    }

    /**
     * 照片语义搜索
     */
    @PostMapping("/search")
    public Result<List<PhotoSearchResult>> searchPhotos(@RequestBody @Valid SearchPhotoRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 验证宠物归属
        photoApplicationService.countPhotos(userId, request.getPetId());
        
        // 调用 Python AI 服务进行语义搜索
        List<PhotoSearchResult> results = pythonAiClient.searchPhotos(request);
        
        log.info("照片语义搜索完成: userId={}, petId={}, query='{}', results={}", 
                userId, request.getPetId(), request.getQuery(), results.size());
        
        return Result.success(results);
    }

    /**
     * 转换为响应对象
     */
    private PhotoResponse toResponse(Photo photo) {
        return PhotoResponse.builder()
                .id(photo.getPhotoId().getValue())
                .petId(photo.getPetId())
                .url(photo.getUrl())
                .thumbnailUrl(photo.getThumbnailUrl())
                .originalUrl(photo.getOriginalUrl())
                .takenAt(photo.getTakenAt())
                .uploadAt(photo.getUploadAt())
                .description(photo.getDescription())
                .tags(photo.getTags())
                .aiTags(photo.getAiTags())
                .aiDescription(photo.getAiDescription())
                .width(photo.getWidth())
                .height(photo.getHeight())
                .fileSize(photo.getFileSize())
                .isCover(photo.getIsCover())
                .createdAt(photo.getCreatedAt())
                .build();
    }
}
