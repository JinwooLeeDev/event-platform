package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginReqDto;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class CompanyAuthController {

    private final CompanyAuthService companyAuthService;

    @PostMapping("/login/company")
    public String login(@ModelAttribute LoginReqDto request, HttpServletRequest servletRequest, HttpSession session) {
        SessionCompanyDto sessionCompany = companyAuthService.login(request);

        // 인증 성공 후 세션 ID 교체
        servletRequest.changeSessionId();

        // 이전 개인 로그인 정보 제거
        session.removeAttribute(LoginSession.USER);

        // 기업 이름 등 화면에 사용할 정보
        session.setAttribute(
                LoginSession.COMPANY,
                sessionCompany
        );

        // 결제·환불 등에서 사용할 공통 로그인 주체
        session.setAttribute(
                LoginSession.IDENTITY,
                LoginIdentity.builder()
                        .id(sessionCompany.getId())
                        .loginType(LoginType.COMPANY)
                        .build()
        );

        return "redirect:/";
    }
}