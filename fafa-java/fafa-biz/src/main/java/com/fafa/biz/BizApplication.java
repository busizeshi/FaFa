package com.fafa.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FaFa 业务服务启动类（fafa-biz）。
 *
 * <p>fafa-biz 是首版产品业务服务，承载全部业务事实（架构规范 §4.3）。
 * 代码按 bounded context 组织，首版包含以下上下文（随模块开发逐步建立包）：</p>
 * <ul>
 *   <li>identity：宠主账号、邮箱验证、家庭成员关系与资源授权</li>
 *   <li>family：家庭、邀请、宠物档案与生命周期</li>
 *   <li>care：照护计划、提醒、任务与真实照护事件</li>
 *   <li>food：食品卡、库存、喂养计划与实际投喂</li>
 *   <li>media：上传会话、媒体归属、回忆与日记</li>
 *   <li>community：公开快照、评论、举报处置</li>
 *   <li>privacy：隐私偏好、导出/删除 job</li>
 *   <li>ai：AI 会话门面、job 与草稿确认</li>
 *   <li>notification：站内通知与邮件发送</li>
 * </ul>
 *
 * <p>MyBatis Mapper 统一注册：仅扫描标注了 {@code @Mapper} 的接口，
 * 且必须位于各上下文的 {@code infrastructure.persistence.mapper} 包内。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.fafa.biz", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class BizApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizApplication.class, args);
    }
}
