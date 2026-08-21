package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.photo.PhotoId;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.PhotoRepository;
import com.fafa.infrastructure.client.PythonAiClient;
import com.fafa.infrastructure.mq.MqProducerService;
import com.fafa.infrastructure.mq.PhotoAnalysisMessage;
import com.fafa.infrastructure.oss.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 照片应用服务
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Slf4j
@Service
public class PhotoApplicationService {

    @Resource
    private PhotoRepository photoRepository;

    @Resource
    private PetRepository petRepository;

    @Resource
    private OssService ossService;

    @Resource
    private MqProducerService mqProducerService;

    @Resource
    private PythonAiClient pythonAiClient;

    @Resource
    private UserTagApplicationService userTagApplicationService;

    /**
     * 上传照片/视频（支持批量上传，支持标签，petId可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> uploadMedia(Long userId, Long petId, List<MultipartFile> files,
                                   LocalDateTime takenAt, String description, List<String> tags) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        if (files.size() > 20) {
            throw new BusinessException("单次最多上传20个文件");
        }

        List<Long> photoIds = new ArrayList<>();

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            String mediaType;
            
            if (contentType != null && contentType.startsWith("video/")) {
                mediaType = "video";
                if (file.getSize() > 50 * 1024 * 1024) {
                    throw new BusinessException("视频大小不能超过50MB");
                }
            } else if (contentType != null && contentType.startsWith("image/")) {
                mediaType = "image";
                if (file.getSize() > 10 * 1024 * 1024) {
                    throw new BusinessException("图片大小不能超过10MB");
                }
            } else {
                throw new BusinessException("不支持的文件类型");
            }

            Integer width = null;
            Integer height = null;
            if ("image".equals(mediaType)) {
                try {
                    BufferedImage image = ImageIO.read(file.getInputStream());
                    if (image != null) {
                        width = image.getWidth();
                        height = image.getHeight();
                    }
                } catch (IOException e) {
                    log.warn("读取图片尺寸失败: {}", e.getMessage());
                }
            }

            String originalUrl = ossService.uploadFile(file, mediaType.equals("video") ? "videos" : "photos");
            String thumbnailUrl = originalUrl;

            Photo photo = Photo.create(petId, userId, originalUrl, thumbnailUrl,
                    takenAt != null ? takenAt : LocalDateTime.now(),
                    description, mediaType);
            photo.updateMetadata(width, height, file.getSize(), originalUrl);

            if (tags != null && !tags.isEmpty()) {
                photo.setTags(tags);
                userTagApplicationService.getOrCreateTags(userId, tags, "photo");
                userTagApplicationService.incrementTagUsage(userId, tags);
            }

            Photo savedPhoto = photoRepository.save(photo);
            photoIds.add(savedPhoto.getPhotoId().getValue());

            try {
                PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
                        .photoId(savedPhoto.getPhotoId().getValue())
                        .petId(petId)
                        .userId(userId)
                        .url(originalUrl)
                        .thumbnailUrl(thumbnailUrl)
                        .mediaType(mediaType)
                        .takenAt(savedPhoto.getTakenAt().toString())
                        .tags(tags)
                        .build();

                mqProducerService.sendPhotoAnalysisMessage(message);
            } catch (Exception e) {
                log.error("发送照片分析消息失败，photoId={}", savedPhoto.getPhotoId().getValue(), e);
            }

            log.info("{}上传成功，photoId={}, petId={}, userId={}",
                    mediaType.equals("video") ? "视频" : "照片",
                    savedPhoto.getPhotoId().getValue(), petId, userId);
        }

        return photoIds;
    }

    /**
     * 上传照片（旧版本，保持兼容）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long uploadPhoto(Long userId, Long petId, MultipartFile file,
                           LocalDateTime takenAt, String description) {
        List<Long> photoIds = uploadMedia(userId, petId, List.of(file), takenAt, description, null);
        return photoIds.get(0);
    }

    /**
     * 更新照片的宠物关联和置信度
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePhotoRecognition(Long photoId, Long petId, BigDecimal confidence) {
        Photo photo = photoRepository.findById(PhotoId.of(photoId))
                .orElseThrow(() -> new BusinessException("照片不存在"));

        photo.markAsAutoRecognized(petId, confidence);
        photoRepository.save(photo);

        log.info("更新照片识别结果，photoId={}, petId={}, confidence={}", photoId, petId, confidence);
    }

    /**
     * 查询照片列表
     */
    public List<Photo> listPhotos(Long userId, Long petId, Integer pageNum, Integer pageSize) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }

        return photoRepository.findByPetId(petId, pageNum, pageSize);
    }

    /**
     * 根据日期范围查询照片
     */
    public List<Photo> listPhotosByDateRange(Long userId, Long petId, 
                                             LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }

        return photoRepository.findByPetIdAndDateRange(petId, startDate, endDate);
    }

    /**
     * 查询照片详情
     */
    public Photo getPhoto(Long userId, Long photoId) {
        Photo photo = photoRepository.findById(PhotoId.of(photoId))
                .orElseThrow(() -> new BusinessException("照片不存在"));
        
        // 验证归属
        Pet pet = petRepository.findById(new PetId(photo.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该照片");
        }

        return photo;
    }

    /**
     * 更新照片描述
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePhotoDescription(Long userId, Long photoId, String description) {
        Photo photo = photoRepository.findById(PhotoId.of(photoId))
                .orElseThrow(() -> new BusinessException("照片不存在"));
        
        // 验证归属
        Pet pet = petRepository.findById(new PetId(photo.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该照片");
        }

        photo.updateDescription(description);
        photoRepository.save(photo);
        
        log.info("更新照片描述成功，photoId={}, userId={}", photoId, userId);
    }

    /**
     * 设置封面照片
     */
    @Transactional(rollbackFor = Exception.class)
    public void setCoverPhoto(Long userId, Long photoId) {
        Photo photo = photoRepository.findById(PhotoId.of(photoId))
                .orElseThrow(() -> new BusinessException("照片不存在"));
        
        // 验证归属
        Pet pet = petRepository.findById(new PetId(photo.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该照片");
        }

        // 取消该宠物的所有封面
        photoRepository.unsetAllCoversByPetId(photo.getPetId());
        
        // 设置新封面
        photo.setAsCover();
        photoRepository.save(photo);
        
        log.info("设置封面照片成功，photoId={}, petId={}, userId={}", photoId, photo.getPetId(), userId);
    }

    /**
     * 删除照片
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long userId, Long photoId) {
        Photo photo = photoRepository.findById(PhotoId.of(photoId))
                .orElseThrow(() -> new BusinessException("照片不存在"));
        
        // 验证归属
        Pet pet = petRepository.findById(new PetId(photo.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该照片");
        }

        // 删除数据库记录
        photoRepository.deleteById(PhotoId.of(photoId));

        // 删除 OSS 文件（可选，根据业务需求）
        // ossService.deleteFile(photo.getUrl());

        // 同步删除 Qdrant 向量（尽力而为，失败不影响删除结果）
        pythonAiClient.deleteMediaVector(photo.getEmbeddingId());
        
        log.info("删除照片成功，photoId={}, userId={}", photoId, userId);
    }

    /**
     * 统计宠物的照片数量
     */
    public int countPhotos(Long userId, Long petId) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }

        return photoRepository.countByPetId(petId);
    }
}
