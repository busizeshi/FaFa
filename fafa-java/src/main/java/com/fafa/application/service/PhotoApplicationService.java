package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.photo.PhotoId;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.PhotoRepository;
import com.fafa.infrastructure.mq.MqProducerService;
import com.fafa.infrastructure.mq.PhotoAnalysisMessage;
import com.fafa.infrastructure.oss.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /**
     * 上传照片
     */
    @Transactional(rollbackFor = Exception.class)
    public Long uploadPhoto(Long userId, Long petId, MultipartFile file, 
                           LocalDateTime takenAt, String description) {
        // 1. 验证宠物归属
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }

        // 2. 读取图片元数据
        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (IOException e) {
            log.warn("读取图片尺寸失败: {}", e.getMessage());
        }

        // 3. 上传原图到 OSS
        String originalUrl = ossService.uploadFile(file, "photos");
        
        // 4. 生成缩略图并上传（这里简化处理，实际应该压缩）
        String thumbnailUrl = originalUrl; // 实际应该生成缩略图
        
        // 5. 创建照片记录
        Photo photo = Photo.create(petId, userId, originalUrl, thumbnailUrl, 
                                   takenAt != null ? takenAt : LocalDateTime.now(), 
                                   description);
        photo.updateMetadata(width, height, file.getSize(), originalUrl);
        
        Photo savedPhoto = photoRepository.save(photo);
        
        // 6. 发送 MQ 消息触发 AI 分析
        try {
            PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
                    .photoId(savedPhoto.getPhotoId().getValue())
                    .petId(petId)
                    .userId(userId)
                    .url(originalUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .takenAt(savedPhoto.getTakenAt().toString())
                    .build();
            
            mqProducerService.sendPhotoAnalysisMessage(message);
        } catch (Exception e) {
            log.error("发送照片分析消息失败，photoId={}", savedPhoto.getPhotoId().getValue(), e);
            // 不影响主流程，继续返回
        }
        
        log.info("照片上传成功，photoId={}, petId={}, userId={}", 
                savedPhoto.getPhotoId().getValue(), petId, userId);
        
        return savedPhoto.getPhotoId().getValue();
    }

    /**
     * 查询照片列表
     */
    public List<Photo> listPhotos(Long userId, Long petId, Integer pageNum, Integer pageSize) {
        // 验证宠物归属
        Pet pet = petRepository.findById(petId)
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
        Pet pet = petRepository.findById(petId)
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
        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该照片"));
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
        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该照片"));
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
        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该照片"));
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
        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该照片"));
        }

        // 删除数据库记录
        photoRepository.deleteById(PhotoId.of(photoId));
        
        // 删除 OSS 文件（可选，根据业务需求）
        // ossService.deleteFile(photo.getUrl());
        
        // 删除 Qdrant 向量（后续实现）
        // if (photo.getEmbeddingId() != null) {
        //     deletePhotoEmbedding(photo.getEmbeddingId());
        // }
        
        log.info("删除照片成功，photoId={}, userId={}", photoId, userId);
    }

    /**
     * 统计宠物的照片数量
     */
    public int countPhotos(Long userId, Long petId) {
        // 验证宠物归属
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物"));
        }

        return photoRepository.countByPetId(petId);
    }
}
