package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.photo.PhotoId;
import com.fafa.domain.repository.PhotoRepository;
import com.fafa.infrastructure.persistence.converter.PhotoConverter;
import com.fafa.infrastructure.persistence.dataobject.PhotoDO;
import com.fafa.infrastructure.persistence.mapper.PhotoMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 照片仓储实现
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Repository
public class PhotoRepositoryImpl implements PhotoRepository {

    @Resource
    private PhotoMapper photoMapper;

    @Override
    public Photo save(Photo photo) {
        PhotoDO photoDO = PhotoConverter.toDO(photo);
        
        if (photoDO.getId() == null) {
            photoMapper.insert(photoDO);
        } else {
            photoMapper.updateById(photoDO);
        }
        
        return PhotoConverter.toDomain(photoMapper.selectById(photoDO.getId()));
    }

    @Override
    public Optional<Photo> findById(PhotoId photoId) {
        PhotoDO photoDO = photoMapper.selectById(photoId.getValue());
        return Optional.ofNullable(PhotoConverter.toDomain(photoDO));
    }

    @Override
    public List<Photo> findByPetId(Long petId, Integer pageNum, Integer pageSize) {
        Page<PhotoDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PhotoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PhotoDO::getPetId, petId)
                .orderByDesc(PhotoDO::getTakenAt)
                .orderByDesc(PhotoDO::getCreatedAt);
        
        Page<PhotoDO> resultPage = photoMapper.selectPage(page, queryWrapper);
        return resultPage.getRecords().stream()
                .map(PhotoConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Photo> findByPetIdAndDateRange(Long petId, LocalDate startDate, LocalDate endDate) {
        List<PhotoDO> photoDOList = photoMapper.selectByPetIdAndDateRange(petId, startDate, endDate);
        return photoDOList.stream()
                .map(PhotoConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Photo> findCoverByPetId(Long petId) {
        LambdaQueryWrapper<PhotoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PhotoDO::getPetId, petId)
                .eq(PhotoDO::getIsCover, 1)
                .last("LIMIT 1");
        
        PhotoDO photoDO = photoMapper.selectOne(queryWrapper);
        return Optional.ofNullable(PhotoConverter.toDomain(photoDO));
    }

    @Override
    public int countByPetId(Long petId) {
        LambdaQueryWrapper<PhotoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PhotoDO::getPetId, petId);
        return photoMapper.selectCount(queryWrapper).intValue();
    }

    @Override
    public void deleteById(PhotoId photoId) {
        photoMapper.deleteById(photoId.getValue());
    }

    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<PhotoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PhotoDO::getPetId, petId);
        photoMapper.delete(queryWrapper);
    }

    @Override
    public void unsetAllCoversByPetId(Long petId) {
        photoMapper.unsetAllCoversByPetId(petId);
    }
}
