package com.busan.eventplatform.login;

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
    public static final String SESSION_USER = "sessionUser";

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
    public String login(@ModelAttribute LoginReqDto request, HttpServletRequest servletRequest, HttpSession session
    ) {
        // 이메일·비밀번호·회원 상태 검증
        SessionUserDto sessionUser = authService.login(request);
        // 로그인 성공 시 세션 ID 교체
        servletRequest.changeSessionId();

        session.setAttribute(SESSION_USER, sessionUser);

        // TODO 로그인할 sessionUser가 "User" 인지 , "Company"인지 구분해야함.
//        session.setAttribute("clientType", Client.ClientType.USER);
//        session.setAttribute("clientType", Client.ClientType.COMPANY);


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
