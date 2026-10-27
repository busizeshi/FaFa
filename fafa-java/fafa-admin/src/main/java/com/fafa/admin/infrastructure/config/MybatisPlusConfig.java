package com.fafa.admin.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 框架装配（fafa-admin）。
 *
 * <p>与 fafa-biz 保持一致的插件基线：分页上限 + 防全表攻击，
 * 配置原因详见 {@code com.fafa.biz.infrastructure.config.MybatisPlusConfig}。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /** 运营列表单页上限：审计/队列类列表建议走 cursor 分页 */
    private static final long MAX_PAGE_SIZE = 100L;

    /**
     * 注册插件链：防全表攻击 → 分页。
     *
     * @return 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        pagination.setOverflow(true);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
