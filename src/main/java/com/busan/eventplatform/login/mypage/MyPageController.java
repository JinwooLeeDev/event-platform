package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.login.personallogin.AuthController;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/mypage")
    public String myPage(@SessionAttribute(name = AuthController.SESSION_USER, required = false) SessionUserDto sessionUser, Model model) {
        if (sessionUser == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        MyPageResDto myPage = myPageService.getMyPage(sessionUser.getId());
        model.addAttribute("myPage", myPage);
        return "login/mypage";
    }

    @GetMapping("/myhistory")
    public String myHistory() {
        return "user/myhistory";
    }

    @GetMapping("/mypoint")
    public String myPoint(
            @SessionAttribute(name = AuthController.SESSION_USER, required = false)
            SessionUserDto sessionUser,
            Model model) {
        if (sessionUser == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        MyPageResDto myPage = myPageService.getMyPage(sessionUser.getId());
        model.addAttribute("myPage", myPage);
        return "user/mypoint";
    }

    @PostMapping("/mypage")
    public String updateMyPage(
            @SessionAttribute(name = AuthController.SESSION_USER, required = false)
            SessionUserDto sessionUser,
            @ModelAttribute MyPageUpdateReqDto request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (sessionUser == null) {throw new Exception401("로그인 후 이용해주세요.");}

        SessionUserDto updatedUser = myPageService.updateMyPage(
                        sessionUser.getId(),
                        request
        );

        // DB 커밋 성공 후 현재 세션의 이름·언어 갱신
        session.setAttribute(
                AuthController.SESSION_USER,
                updatedUser
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "회원 정보가 수정되었습니다."
        );
        return "redirect:/mypage";
    }
}
