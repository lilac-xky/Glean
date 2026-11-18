package com.lilac.config;

import com.lilac.auth.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册鉴权拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // 拦截所有路径
                .addPathPatterns("/**")
                // 白名单：路径不含 context-path（/api 已在 server.servlet 配置）
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/error"
                );
    }
}
