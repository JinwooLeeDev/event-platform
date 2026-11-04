package com.busan.eventplatform.global.error.config;

import com.busan.eventplatform.global.error.interceptor.AdminInterceptor;
import com.busan.eventplatform.global.error.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 로그인 검사
        registry.addInterceptor(loginInterceptor)
                // 인터셉터가 동작할 URL 패턴을 지정
                .addPathPatterns("/user/**", "/board/**", "/reply/**", "/admin/**", "/refund/**")
                // 인터셉터에서 제외할 URL 패턴을 지정
                .excludePathPatterns("/board/list", "/board/{id:\\d+}");
        // Todo - 로그인 인터셉터가 동작할 URL은 행사신청, 마이페이지, 포인트 결제 등등
        //      - 제외할 URL 추가되면 URL 수정
        //      -

        // 2. 관리자 검사 : 로그인 검사 다음에 등록 (등록한 순서대로 실행 됨)
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
        // Todo - 관리자 대시보드 URL 정해지면 수정
    }
}
