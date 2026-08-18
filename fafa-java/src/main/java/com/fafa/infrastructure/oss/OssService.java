package com.fafa.infrastructure.oss;

import cn.hutool.core.util.IdUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.infrastructure.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * OSS 对象存储服务
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件
     * 
     * @param file 文件
     * @param folder 文件夹（如: pets/avatars）
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 确保 Bucket 存在
            ensureBucketExists();

            // 生成文件名: folder/yyyyMM/uuid.ext
            String fileName = generateFileName(file.getOriginalFilename(), folder);

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回访问 URL
            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    /**
     * 上传输入流
     * 
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param contentType 内容类型
     * @return 文件访问 URL
     */
    public String uploadStream(InputStream inputStream, String fileName, String contentType, long size) {
        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("流上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    /**
     * 删除文件
     * 
     * @param fileName 文件名
     */
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new BusinessException("文件删除失败");
        }
    }

    /**
     * 获取文件访问 URL（临时，7天有效）
     * 
     * @param fileName 文件名
     * @return 访问 URL
     */
    public String getFileUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件URL失败: {}", e.getMessage(), e);
            throw new BusinessException("获取文件URL失败");
        }
    }

    /**
     * 确保 Bucket 存在
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("创建 Bucket: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("检查 Bucket 失败: {}", e.getMessage(), e);
            throw new BusinessException("OSS 服务异常");
        }
    }

    /**
     * 生成文件名
     * 
     * @param originalFilename 原始文件名
     * @param folder 文件夹
     * @return 文件名
     */
    private String generateFileName(String originalFilename, String folder) {
        // 获取文件扩展名
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 生成日期路径: yyyyMM
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 生成文件名: folder/yyyyMM/uuid.ext
        return String.format("%s/%s/%s%s", folder, datePath, IdUtil.simpleUUID(), extension);
    }
}
