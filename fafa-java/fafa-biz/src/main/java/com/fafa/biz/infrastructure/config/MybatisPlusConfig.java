package com.fafa.biz.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 框架装配（仅基础设施层可见的技术配置）。
 *
 * <p>装配内容（架构规范 §3 / §8.2）：</p>
 * <ul>
 *   <li>分页插件：分页上限由服务端配置约束，禁止客户端无限 pageSize；</li>
 *   <li>防全表攻击插件：拦截未带 where 条件的 update/delete，防止误操作清表。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 单条记录最大分页大小：超出将被截断（业务如需大分页应改走 cursor 游标分页）。
     */
    private static final long MAX_PAGE_SIZE = 200L;

    /**
     * 注册 MyBatis-Plus 内置插件链。
     *
     * <p>插件注册顺序有讲究：防全表攻击插件先于分页插件执行，
     * 确保危险 SQL 在分页改写前即被拦截。</p>
     *
     * @return 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 防全表 update/delete：不带 where 条件的语句直接抛异常（Fail-fast 优于事后补偿）
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 2. 分页插件：指定 MySQL 方言，并限制单页上限
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(MAX_PAGE_SIZE);
        pagination.setOverflow(true); // 页码超界时回到首页，避免空页引发前端误判
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
