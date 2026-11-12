package com.busan.eventplatform.global.error.config;

import com.busan.eventplatform.global.error.interceptor.AdminInterceptor;
import com.busan.eventplatform.global.error.interceptor.CompanyInterceptor;
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
    private final CompanyInterceptor companyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 로그인 검사
        registry.addInterceptor(loginInterceptor)
                // 인터셉터가 동작할 URL 패턴을 지정
                .addPathPatterns("/user/**", "/board/**", "/reply/**", "/admin", "/admin/**", "/refund/**")
                // 인터셉터에서 제외할 URL 패턴을 지정
                .excludePathPatterns("/board/list", "/board/{id:\\d+}");
        // Todo - 로그인 인터셉터가 동작할 URL은 행사신청, 마이페이지, 포인트 결제 등등
        //      - 제외할 URL 추가되면 URL 수정
        // 2. 기업권한 검사
        registry.addInterceptor(companyInterceptor)
                // Todo - 경로들은 모두 추후에 재작성 필요
                .addPathPatterns("/company/**")
                .excludePathPatterns("");

        // 2. 관리자 검사 : 로그인 검사 다음에 등록 (등록한 순서대로 실행 됨)
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin", "/admin/**");
    }
}
