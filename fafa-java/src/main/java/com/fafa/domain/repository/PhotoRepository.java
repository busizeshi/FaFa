package com.fafa.domain.repository;

import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.photo.PhotoId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 照片仓储接口
 *
 * @author FaFa
 * @since 2026-08-18
 */
public interface PhotoRepository {

    /**
     * 保存照片
     */
    Photo save(Photo photo);

    /**
     * 根据ID查询照片
     */
    Optional<Photo> findById(PhotoId photoId);

    /**
     * 根据宠物ID查询照片列表（分页）
     */
    List<Photo> findByPetId(Long petId, Integer pageNum, Integer pageSize);

    /**
     * 根据宠物ID和日期范围查询照片
     */
    List<Photo> findByPetIdAndDateRange(Long petId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询宠物的封面照片
     */
    Optional<Photo> findCoverByPetId(Long petId);

    /**
     * 统计宠物的照片数量
     */
    int countByPetId(Long petId);

    /**
     * 删除照片
     */
    void deleteById(PhotoId photoId);

    /**
     * 取消宠物的所有封面照片
     */
    void unsetAllCoversByPetId(Long petId);
}
