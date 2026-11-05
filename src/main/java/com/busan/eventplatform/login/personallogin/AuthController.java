package com.busan.eventplatform.login.personallogin;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginReqDto;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;

import com.busan.eventplatform.global.util.Define;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    // 인터셉터에서도 같은 키를 사용해야 합니다.

    public static final String SESSION_USER = LoginSession.USER;


    private final AuthService authService;

    // 회원가입 화면
    @GetMapping("/signup")
    public String signupForm() {
        return "login/signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@ModelAttribute SignupReqDto request, HttpSession session) {
        // 같은 세션의 가입 요청끼리 인증 결과를 중복 사용하지 않도록 처리
        synchronized (session) {
            authService.signup(request, session);
            // 회원 저장 트랜잭션이 정상 커밋된 뒤 인증 정보 제거
            session.removeAttribute(Define.EMAIL_VERIFICATION);
        }
        return "redirect:/login";
    }

    // 로그인 화면
    @GetMapping("/login")
    public String loginForm() {
        return "login/login";
    }

    // 로그인 처리
    @PostMapping("/login")
    public String login(@ModelAttribute LoginReqDto request, HttpServletRequest servletRequest, HttpSession session) {
        SessionUserDto sessionUser = authService.login(request);

        servletRequest.changeSessionId();

        // 이전 기업 로그인 정보 제거
        session.removeAttribute(LoginSession.COMPANY);

        // 헤더 등에서 사용하는 개인 회원 정보
        session.setAttribute(LoginSession.USER, sessionUser);

        // 결제·환불 등에서 공통으로 사용하는 로그인 주체
        session.setAttribute(
                LoginSession.IDENTITY, // identity - id랑 logintype
                LoginIdentity.builder()
                        .id(sessionUser.getId())
                        .loginType(LoginType.USER)
                        .build()
        );
        return "redirect:/";
    }

    // 로그아웃 처리
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {session.invalidate();}

        return "redirect:/login";
    }
}
