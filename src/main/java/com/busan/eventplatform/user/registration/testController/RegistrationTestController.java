package com.busan.eventplatform.user.registration.testController;

import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional; // 👈 임포트 확인

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true) // 👈 클래스 상단에 추가하여 뷰 렌더링 시점까지 DB 세션 유지!
public class RegistrationTestController {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @GetMapping("/test/mypage")
    public String testMyPage(Model model) {

        // 1. DB에 저장된 모든 참가 신청 내역 조회
        List<Registration> allRegistrations = registrationRepository.findAll();

        // 🔍 디버깅용 콘솔 출력 (인텔리제이 Run 창을 확인해보세요!)
        System.out.println("=== [디버깅] 총 신청 내역 개수: " + allRegistrations.size());
        for (Registration reg : allRegistrations) {
            System.out.println(" - 신청 ID: " + reg.getId() + ", 유저 ID: " + reg.getUser().getId() + ", 행사 제목: " + reg.getSession().getEvent().getTitle());
        }

        final Long targetUserId;
        if (!allRegistrations.isEmpty()) {
            targetUserId = allRegistrations.get(allRegistrations.size() - 1).getUser().getId();
        } else {
            targetUserId = 1L;
        }

        List<Registration> myRegistrations = allRegistrations.stream()
                .filter(reg -> reg.getUser().getId().equals(targetUserId))
                .toList();

        // 2. 실제 유저 정보 조회
        User targetUser = userRepository.findById(targetUserId).orElse(null);

        if (targetUser != null) {
            model.addAttribute("myPage", Map.of(
                    "displayName", targetUser.getDisplayName(),
                    "point", targetUser.getPoint(),
                    "email", targetUser.getEmailNormalized(),
                    "preferredLocale", targetUser.getPreferredLocale() != null ? targetUser.getPreferredLocale() : "ko",
                    "createdAt", targetUser.getCreatedAt() != null ? targetUser.getCreatedAt().toString().substring(0, 10) : "2026-11-09"
            ));
        } else {
            model.addAttribute("myPage", Map.of(
                    "displayName", "박지훈",
                    "point", 60000,
                    "email", "jihun@naver.com",
                    "preferredLocale", "ko",
                    "createdAt", "2026-11-09"
            ));
        }

        model.addAttribute("registrations", myRegistrations);

        return "login/mypage";
    }

    /**
     * [테스트 전용 URL] 행사 상세 페이지 렌더링 테스트
     */
    @GetMapping("/test/events/{eventId}")
    public String testEventDetail(@PathVariable("eventId") Long eventId, Model model) {
        model.addAttribute("event", Map.of(
                "id", eventId,
                "title", "2026 부산 벡스코 IT 컨퍼런스",
                "content", "스프링 부트와 프론트엔드 연동을 위한 실전 프로젝트 세션입니다.",
                "fee", 10000,
                "capacity", 50
        ));
        model.addAttribute("confirmedCount", 12);
        return "event/detail";
    }
}