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
            @RequestParam(defaultValue = "confirmed") String tab,
            @RequestParam(defaultValue = "") String keyword,
            Model model
    ) {
        prepareHistory(identity, tab, keyword, model);
        return "user/myhistory";
    }

    // ★ [f/41] 탭 전환·검색 비동기용 — 신청 내역 영역(history-content)만 응답 (페이지 전체 새로고침 없음)
    @GetMapping("/myhistory/content")
    public String myHistoryContent(
            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
            @RequestParam(defaultValue = "confirmed") String tab,
            @RequestParam(defaultValue = "") String keyword,
            Model model
    ) {
        prepareHistory(identity, tab, keyword, model);
        return "user/history-content";
    }

    // [나의 신청 내역] 페이지·비동기 조각이 같은 데이터를 쓰도록 한 곳에서 준비
    private void prepareHistory(LoginIdentity identity, String tab, String keyword, Model model) {
        if (identity == null || identity.getId() == null || identity.getLoginType() == null) {
            throw new Exception401("로그인 후 이용해주세요.");
        }
        if (!identity.isUser()) {
            throw new Exception403("개인 회원만 신청 내역을 조회할 수 있습니다.");
        }
        // ★ [f/41] 탭 값 정리는 서비스가 담당 (confirmed / waiting / past / cancelled, 그 외는 confirmed)
        var history = registrationService.getMyHistory(identity, tab, keyword);
        String currentTab = history.getCurrentTab();
        model.addAttribute("history", history);
        model.addAttribute("myPoint", myPageService.getMyPage(identity.getId()).getPoint());
        model.addAttribute("resultCount", history.getItems().size());
        String search = "&keyword=" + URLEncoder.encode(history.getKeyword(), StandardCharsets.UTF_8);
        model.addAttribute("historyTabs", List.of(
                Map.of("url", "/myhistory?tab=confirmed" + search, "label", "참가 확정", "count", history.getConfirmedCount(), "active", currentTab.equals("confirmed")),
                Map.of("url", "/myhistory?tab=waiting" + search, "label", "대기 중", "count", history.getWaitingCount(), "active", currentTab.equals("waiting")),
                Map.of("url", "/myhistory?tab=past" + search, "label", "종료된 행사", "count", history.getPastCount(), "active", currentTab.equals("past")),
                // ★ [f/41] 취소·거절·행사 취소 내역 (환불 확인용, 담당자 요청)
                Map.of("url", "/myhistory?tab=cancelled" + search, "label", "취소·환불", "count", history.getCancelledCount(), "active", currentTab.equals("cancelled"))));
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

        model.addAttribute("pointHistory", purchaseService.pointHistory(sessionUser.getId()));
        model.addAttribute("pointSize", purchaseService.pointHistory(sessionUser.getId()).size());

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
