package com.busan.eventplatform.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 세션 정보를 머스타치 코드로 활용하기위함
 * 헤더에 "사용자 이름"님 을 띄우기 위한 클래스
 */
@ControllerAdvice
public class LoginViewAdvice {
    @ModelAttribute
    public void addLoginUser(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object value = session.getAttribute(AuthController.SESSION_USER);
        if (!(value instanceof SessionUserDto user)) return;
        model.addAttribute("loginUser", user);
//        Todo - User 엔티티 클래스 완성 이후 주석 해제
        model.addAttribute("isAdmin", user.isAdmin());
        model.addAttribute("isPersonal", user.isMember());
    }
}
