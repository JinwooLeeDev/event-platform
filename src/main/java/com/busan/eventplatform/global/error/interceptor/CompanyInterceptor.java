package com.busan.eventplatform.global.error.interceptor;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
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
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);
        // 개인 계정 중 관리자만 허용
        if (loginIdentity.getLoginType() == LoginType.USER) {
            Object value = session.getAttribute(LoginSession.USER);
            if (!(value instanceof SessionUserDto user)
                    || !java.util.Objects.equals(loginIdentity.getId(), user.getId())) {
                throw new Exception401("다시 로그인해주세요.");
            }
            if (!user.isAdmin()) {
                throw new Exception403("기업회원 또는 관리자만 접근할 수 있습니다.");
            }
            return true;
        }

        // 기업 계정은 허용
        if (loginIdentity.getLoginType() == LoginType.COMPANY) {
            Object value = session.getAttribute(LoginSession.COMPANY);
            if (!(value instanceof SessionCompanyDto company)
                    || !java.util.Objects.equals(loginIdentity.getId(), company.getId())) {
                throw new Exception401("기업 계정으로 다시 로그인해주세요.");
            }
            return true;
        }
        throw new Exception403("접근 권한이 없습니다.");
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
