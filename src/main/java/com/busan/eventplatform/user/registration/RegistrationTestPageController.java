package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * ⚠ [임시] 참가 신청 버튼 연동 확인용 테스트 상세 화면 — PR 전 삭제
 *
 * 행사 담당자의 실제 상세 화면(/event/{id})에 회차 목록이 아직 없어서,
 * 공용 신청 버튼(registration/apply-button-script)을 눌러 볼 수 있는 최소 화면을 따로 둔다.
 * 템플릿 이름을 event/detail 로 하면 행사 담당자 화면과 겹치므로 registration/test-event-detail 사용.
 */
@Controller
@RequiredArgsConstructor
public class RegistrationTestPageController {

    private final EventSessionRepository eventSessionRepository;
    private final RegistrationService registrationService;

    // 회차 → 행사(LAZY) 정보를 꺼내는 동안 DB 세션 유지 (open-in-view=false 환경, 임시 화면 전용)
    @Transactional(readOnly = true)
    @GetMapping("/test/events/{eventId}")
    public String detail(@PathVariable Long eventId, Model model) {
        // 행사의 회차 목록 (시작 시각 순)
        List<EventSession> sessions = eventSessionRepository.findByEventId(eventId).stream()
                .sorted(Comparator.comparing(EventSession::getStartsAt))
                .toList();
        if (sessions.isEmpty()) {
            throw new Exception404("회차가 없는 행사입니다");
        }

        // 회차별 화면 표시 값 — Map.of 는 null 을 허용하지 않으므로 모든 값을 빈 문자열·숫자로 채운다
        List<Map<String, Object>> rows = sessions.stream()
                .map(s -> {
                    String closedReason = registrationService.getClosedReason(s); // 신청 불가 사유 (가능하면 null)
                    int remaining = Math.max(0, s.getCapacity() - s.getOccupiedCount());
                    return Map.<String, Object>of(
                            "id", s.getId(),
                            "name", s.getName() == null ? "" : s.getName(),
                            "schedule", RegistrationRes.formatSchedule(s.getStartsAt(), s.getEndsAt()),
                            "feeText", String.format("%,dP", s.getFee()),
                            "remainingText", remaining > 0 ? "잔여 " + remaining + "석" : "정원 마감 (대기 신청 가능)",
                            "canApply", closedReason == null,
                            "closedReason", closedReason == null ? "" : closedReason);
                })
                .toList();

        model.addAttribute("title", sessions.get(0).getEvent().getTitle());
        model.addAttribute("sessions", rows);
        return "registration/test-event-detail";
    }
}
