package com.fafa.infrastructure.oss;

import cn.hutool.core.util.IdUtil;
import com.fafa.infrastructure.config.FaFaProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 腾讯云 COS 存储实现
 *
 * 对象路径规范：{folder}/{yyyyMM}/{uuid}.{ext}
 * 桶与访问权限（公共读供直链展示/AI 拉图）由用户在 COS 控制台预先维护，启动时不做自动建桶。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.storage.type", havingValue = "cos")
public class CosStorageService implements StorageService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final COSClient cosClient;
    private final FaFaProperties properties;

    @PostConstruct
    public void init() {
        FaFaProperties.Cos cos = properties.getCos();
        log.info("COS 存储就绪: https://{}.cos.{}.myqcloud.com", cos.getBucketName(), cos.getRegion());
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                : "jpg";

        String objectKey = buildObjectKey(folder, extension);
        FaFaProperties.Cos cos = properties.getCos();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            cosClient.putObject(cos.getBucketName(), objectKey, file.getInputStream(), metadata);

            return "https://" + cos.getBucketName() + ".cos." + cos.getRegion() + ".myqcloud.com/" + objectKey;

        } catch (Exception ex) {
            log.error("COS 上传失败: objectKey={}", objectKey, ex);
            throw new RuntimeException("File upload failed", ex);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            cosClient.deleteObject(properties.getCos().getBucketName(), objectKey);
        } catch (Exception ex) {
            log.error("COS 删除失败: fileUrl={}", fileUrl, ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl, int expireSeconds) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    properties.getCos().getBucketName(), objectKey, HttpMethodName.GET);
            request.setExpiration(expiration);

            return cosClient.generatePresignedUrl(request).toString();

        } catch (Exception ex) {
            log.error("COS 预签名失败: fileUrl={}", fileUrl, ex);
            throw new RuntimeException("Generate presigned URL failed", ex);
        }
    }

    private String buildObjectKey(String folder, String extension) {
        String month = LocalDate.now().format(MONTH_FORMATTER);
        return folder + "/" + month + "/" + IdUtil.fastSimpleUUID() + "." + extension;
    }

    private String extractObjectKey(String fileUrl) {
        String bucket = properties.getCos().getBucketName();
        int bucketIndex = fileUrl.indexOf(bucket);
        if (bucketIndex == -1) {
            throw new IllegalArgumentException("Invalid COS URL: " + fileUrl);
        }
        return fileUrl.substring(bucketIndex + bucket.length() + 1);
    }
}
