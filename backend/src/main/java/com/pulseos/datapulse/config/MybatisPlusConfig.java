package com.pulseos.datapulse.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 持久层配置。
 * <p>
 * 主要用于注册平台元数据主库（MySQL）的分页拦截器，保证列表分页查询自动拼接 {@code LIMIT} 并限制单页最大条数。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MySQL 分页插件：单页最多只允许查 500 条，防止超大分页请求拖垮数据库。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        // 页码超出总页数时不回绕到首页，直接返回空列表
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
