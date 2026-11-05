package com.busan.eventplatform.global.error.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CompanyInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
//        Todo - 컴퍼니dto 클래스 만들어지면 주석해제
//        SessionCompanyDto sessionCompanyDto = (session == null) ? null : (SessionCompanyDto) session.getAttribute(Define.SESSION_Company);
//
//        // 1. 로그인 확인은 LoginInterceptor 가 먼저 처리 (하지만 등록 순서가 바뀌면 대 참사가 일어 난다)
//        if (sessionCompanyDto == null) {
//            throw new Exception401("로그인 먼저 해주세요");
//        }
//        // 2. 관리자가 아니면 접근 거부
//        if (!sessionCompanyDto.isCompany()) {
//            throw new Exception403("관리자만 접근할 수 있습니다");
//        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
