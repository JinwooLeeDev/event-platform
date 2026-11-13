package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.purchase.PurchaseRes;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.user.registration.RegistrationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final PurchaseService purchaseService;
    private final PaymentService paymentService;
    private final RegistrationService registrationService;

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
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(defaultValue = "") String keyword,
            Model model
    ) {
        if (identity == null || identity.getId() == null || identity.getLoginType() == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        if (!identity.isUser()) {
            throw new Exception403("개인 회원만 신청 내역을 조회할 수 있습니다.");
        }
        String currentTab = tab != null && Set.of("all", "upcoming", "waiting", "past").contains(tab) ? tab : "all";
        var history = registrationService.getMyHistory(identity, currentTab, keyword);
        model.addAttribute("history", history);
        model.addAttribute("myPoint", myPageService.getMyPage(identity.getId()).getPoint());
        model.addAttribute("resultCount", history.getItems().size());
        String search = "&keyword=" + URLEncoder.encode(history.getKeyword(), StandardCharsets.UTF_8);
        model.addAttribute("historyTabs", List.of(
                Map.of("url", "/myhistory?tab=all" + search, "label", "전체 내역", "count", history.getTotalCount(), "active", currentTab.equals("all")),
                Map.of("url", "/myhistory?tab=upcoming" + search, "label", "예정 행사", "count", history.getUpcomingCount(), "active", currentTab.equals("upcoming")),
                Map.of("url", "/myhistory?tab=waiting" + search, "label", "대기 중", "count", history.getWaitingCount(), "active", currentTab.equals("waiting")),
                Map.of("url", "/myhistory?tab=past" + search, "label", "지난 · 취소 내역", "count", history.getPastCount(), "active", currentTab.equals("past"))));
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
