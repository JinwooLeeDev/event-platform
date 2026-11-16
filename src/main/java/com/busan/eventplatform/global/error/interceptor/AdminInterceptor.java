package com.busan.eventplatform.global.error.interceptor;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 확인만 해야 하므로 새로운 세션을 만들면 안됨. false 설정 필수 !
        HttpSession session = request.getSession(false);
        LoginIdentity identity = LoginSession.requireIdentity(session);
        if (!identity.isUser()) {
            throw new Exception403("관리자만 접근할 수 있습니다");
        }
        Object value = session.getAttribute(LoginSession.USER);
        if (!(value instanceof SessionUserDto user)
                || !java.util.Objects.equals(identity.getId(), user.getId())) {
            throw new Exception401("다시 로그인해주세요");
        }
        if (!user.isAdmin()) {
            throw new Exception403("관리자만 접근할 수 있습니다");
        }

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
