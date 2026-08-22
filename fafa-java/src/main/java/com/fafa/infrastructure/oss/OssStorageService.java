package com.fafa.infrastructure.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.fafa.infrastructure.config.FaFaProperties;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 阿里云 OSS 存储实现（生产环境）
 *
 * 对象路径规范：{folder}/{yyyyMM}/{uuid}.{ext}
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final OSS ossClient;
    private final FaFaProperties properties;

    @Override
    public String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1) 
            : "jpg";
        
        String objectKey = buildObjectKey(folder, extension);
        String bucketName = properties.getOss().getBucketName();
        
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName, 
                objectKey, 
                file.getInputStream()
            );
            ossClient.putObject(putObjectRequest);
            
            return "https://" + bucketName + "." + properties.getOss().getEndpoint() + "/" + objectKey;
            
        } catch (Exception ex) {
            log.error("OSS 上传失败: objectKey={}", objectKey, ex);
            throw new RuntimeException("File upload failed", ex);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            ossClient.deleteObject(properties.getOss().getBucketName(), objectKey);
        } catch (Exception ex) {
            log.error("OSS 删除失败: fileUrl={}", fileUrl, ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl, int expireSeconds) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                properties.getOss().getBucketName(), 
                objectKey
            );
            request.setExpiration(expiration);
            
            return ossClient.generatePresignedUrl(request).toString();
            
        } catch (Exception ex) {
            log.error("OSS 预签名失败: fileUrl={}", fileUrl, ex);
            throw new RuntimeException("Generate presigned URL failed", ex);
        }
    }

    private String buildObjectKey(String folder, String extension) {
        String month = LocalDate.now().format(MONTH_FORMATTER);
        return folder + "/" + month + "/" + IdUtil.fastSimpleUUID() + "." + extension;
    }

    private String extractObjectKey(String fileUrl) {
        String bucketName = properties.getOss().getBucketName();
        String endpoint = properties.getOss().getEndpoint();
        String prefix = "https://" + bucketName + "." + endpoint + "/";
        
        if (!fileUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid OSS URL: " + fileUrl);
        }
        
        return fileUrl.substring(prefix.length());
    }
}
