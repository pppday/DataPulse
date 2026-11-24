package com.pulseos.datapulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

/**
 * 全局跨域（CORS）过滤器配置。
 * <p>
 * 允许前后端分离部署时 Vue 前端从不同域名/端口访问后端 API，并显式暴露
 * {@code Content-Disposition} 响应头，保证前端触发 Excel 导出下载时能够正确解析出后端返回的文件名。
 */
@Configuration
public class CorsConfig {

    /**
     * 注册全局生效的 {@link CorsFilter}，放行所有路径的跨域预检与正式请求。
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许携带认证凭证（Cookie / Authorization 头）
        config.setAllowCredentials(true);
        // 放行任意来源域名（使用 Pattern 以兼容 allowCredentials=true）
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // 关键：把 Content-Disposition 暴露给浏览器前端 JS，否则下载 Excel 时拿不到中文文件名
        config.addExposedHeader("Content-Disposition");
        // 预检请求（OPTIONS）缓存 1 小时，减少重复预检开销
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
