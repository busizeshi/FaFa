package com.fafa.infrastructure.oss;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储服务接口（策略模式）
 *
 * 开发环境：MinIO
 * 生产环境：阿里云 OSS
 *
 * @author FaFa Team
 * @since 1.0
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param file   文件
     * @param folder 文件夹（avatar/profile/daily等）
     * @return 文件访问URL
     */
    String upload(MultipartFile file, String folder);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    void delete(String fileUrl);

    /**
     * 生成预签名URL（用于临时访问私有文件）
     *
     * @param fileUrl        文件URL
     * @param expireSeconds  过期时间（秒）
     * @return 预签名URL
     */
    String generatePresignedUrl(String fileUrl, int expireSeconds);
}
