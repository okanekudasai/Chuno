package com.leesfamily.chuno.configure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new LoginInterceptor()) // LoginInterceptor
//                .addPathPatterns("/**") // 적용할 URL (모든 URL 적용)
//                .excludePathPatterns("/kakao/**", "/swagger-ui/**"
//                        ,"/v3/api-docs/**", "/swagger-resources/**"
//                        , "/webjars/**"); // 제외할 URL (/kakao/하위로 오는 URL 제외)

    }
}
