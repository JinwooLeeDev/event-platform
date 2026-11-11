package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.purchase.PurchaseRes;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final PurchaseService purchaseService;
    private final PaymentService paymentService;

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
    public String myHistory(
            @SessionAttribute(
                    name = AuthController.SESSION_USER,
                    required = false
            ) SessionUserDto sessionUser,
            Model model
    ) {
        if (sessionUser == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        model.addAttribute(
                "purchaseList",
                // 회차별 조회 구현 전까지 사용하지 않는 EventSession 인자는 null로 전달
                purchaseService.purchaseList(sessionUser.getId())
        );

        return "user/myhistory";
    }

    @GetMapping("/mypoint")
    public String myPoint(
            @SessionAttribute(
                    name = AuthController.SESSION_USER,
                    required = false
            ) SessionUserDto sessionUser,
            Model model
    ) {
        if (sessionUser == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }

        model.addAttribute(
                "myPage",
                myPageService.getMyPage(sessionUser.getId())
        );

        model.addAttribute(
                "purchaseList",
                purchaseService.purchaseList(sessionUser.getId())
        );

        model.addAttribute("paymentList" , paymentService.paymentList(sessionUser.getId()));

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

    @PostMapping("/mypage/password")
    public String updatePassword(
            @SessionAttribute(name = AuthController.SESSION_USER, required = false)
            SessionUserDto sessionUser,
            @ModelAttribute MyPasswordUpdateReqDto request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (sessionUser == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        try {
            myPageService.updatePassword(sessionUser.getId(), request);
        } catch (Exception400 exception) {
            redirectAttributes.addFlashAttribute("passwordError", exception.getMessage());
            return "redirect:/mypage#password-settings";
        }

        session.invalidate();
        redirectAttributes.addFlashAttribute("passwordChangedMessage",
                "비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해주세요.");
        return "redirect:/login";
    }
}
