package com.fafa.infrastructure.oss;

import java.io.InputStream;

/**
 * 对象存储抽象接口
 *
 * 开发/测试使用 MinIO 实现，生产切换阿里云 OSS 时新增 OssStorageService 实现，
 * 通过 fafa.storage.type 配置决定装配哪个实现，业务代码无感知
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param objectKey   对象路径，规范：{模块}/{yyyyMM}/{uuid}.{ext}
     * @param stream      文件流
     * @param size        文件大小（字节）
     * @param contentType MIME 类型
     * @return 对象 key（业务侧自行拼接访问 URL）
     */
    String upload(String objectKey, InputStream stream, long size, String contentType);

    /**
     * 删除对象。删除失败记日志不抛出（先删库后删文件，文件残留可人工清理）
     */
    void delete(String objectKey);

    /**
     * 生成临时下载链接
     *
     * @param objectKey     对象路径
     * @param expireSeconds 有效期（秒）
     */
    String presignedGetUrl(String objectKey, int expireSeconds);
}
