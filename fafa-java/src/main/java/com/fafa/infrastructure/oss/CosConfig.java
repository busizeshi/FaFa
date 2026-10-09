package com.fafa.infrastructure.oss;

import com.fafa.infrastructure.config.FaFaProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端装配（fafa.storage.type=cos 时生效）
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "fafa.storage.type", havingValue = "cos")
public class CosConfig {

    @Bean
    public COSClient cosClient(FaFaProperties properties) {
        FaFaProperties.Cos cos = properties.getCos();
        log.info("装配腾讯云 COS 存储: bucket={}, region={}", cos.getBucketName(), cos.getRegion());
        COSCredentials credentials = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }
}
