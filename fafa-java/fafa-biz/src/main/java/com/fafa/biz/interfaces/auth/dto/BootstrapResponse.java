package com.fafa.biz.interfaces.auth.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 启动引导响应（契约 BOOTSTRAP GET /bootstrap，公开接口）。
 *
 * <p>客户端据此决定协议弹窗、功能开关与上传能力；字典版本供本地缓存失效。</p>
 *
 * @param featureFlags       功能开关
 * @param legalVersions      当前生效协议版本（客户端内置文本对齐后决定是否重新弹同意）
 * @param uploadCapabilities 上传能力（大小/格式上限，M06 使用，先给默认）
 * @param dictionaryVersions 字典版本（品种表等，M01 使用）
 * @param serverTime         服务器 UTC 时间（ISO 8601，客户端时钟校准参考）
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record BootstrapResponse(
        Map<String, Boolean> featureFlags,
        Map<String, String> legalVersions,
        Map<String, Object> uploadCapabilities,
        Map<String, Integer> dictionaryVersions,
        LocalDateTime serverTime) {

    /**
     * @return 当前 M00 阶段的功能开关快照
     */
    public static BootstrapResponse m00Defaults() {
        return new BootstrapResponse(
                Map.of(
                        "community", true,
                        "ai", true,
                        "foodInventory", true,
                        "reportExport", true
                ),
                // 版本由 AuthProperties 配置注入（见 AuthController）
                Map.of(),
                Map.of(
                        "maxImageBytes", 20_971_520L,
                        "maxVideoBytes", 524_288_000L,
                        "imageFormats", List.of("jpg", "png", "heic", "webp")
                ),
                Map.of(
                        "catBreeds", 0,
                        "dogBreeds", 0
                ),
                LocalDateTime.now(java.time.ZoneOffset.UTC));
    }
}
