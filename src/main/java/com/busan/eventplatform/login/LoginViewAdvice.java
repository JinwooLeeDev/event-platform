package com.busan.eventplatform.login;

import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.login.personallogin.SessionUserDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LoginViewAdvice {

    @ModelAttribute
    public void addLoginUser(HttpServletRequest request, Model model) {
        model.addAttribute("isAdmin", false); // 관리자
        model.addAttribute("isPersonal", false); // 개인회원
        model.addAttribute("isCorporate", false); // 주최측 (기업측)

        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        Object value = session.getAttribute(
                LoginSession.IDENTITY
        );

        if (!(value instanceof LoginIdentity identity)) {
            return;
        }

        if (identity.getLoginType() == LoginType.USER) {
            Object userValue = session.getAttribute(
                    LoginSession.USER
            );

            if (userValue instanceof SessionUserDto user
                    && java.util.Objects.equals(
                    identity.getId(),
                    user.getId()
            )) {
                model.addAttribute("loginUser", user);
                model.addAttribute("isAdmin", user.isAdmin());
                model.addAttribute("isPersonal", user.isMember());
            }

        } else if (identity.getLoginType() == LoginType.COMPANY) {
            Object companyValue = session.getAttribute(
                    LoginSession.COMPANY
            );

            if (companyValue instanceof SessionCompanyDto company
                    && java.util.Objects.equals(
                    identity.getId(),
                    company.getId()
            )) {
                model.addAttribute("loginUser", company);
                model.addAttribute("isCorporate", true);
            }
        }
    }
}