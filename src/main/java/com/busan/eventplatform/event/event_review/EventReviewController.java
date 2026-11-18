package com.busan.eventplatform.event.event_review;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class EventReviewController {

    private final EventReviewService eventReviewService;

    // 특정 회차 후기 목록 조회
    // GET - http://localhost:8080/event-review/1
    @GetMapping("/event-review/{eventSessionId}")
    public String list(@PathVariable Long eventSessionId,
                       @RequestParam(defaultValue = "latest") String sort,
                       Model model) {

        List<EventReviewResponse.ListDto> reviewList =
                eventReviewService.findAllByEventSessionId(eventSessionId, sort)
                        .stream()
                        .map(EventReviewResponse.ListDto::new)
                        .toList();

        model.addAttribute("reviewList", reviewList);
        model.addAttribute("sort", sort);

        return "event/review-list";
    }

    // 후기 작성
    // POST - http://localhost:8080/event-review/save/1
    @PostMapping("/event-review/save/{eventSessionId}")
    public String save(
            @PathVariable Long eventSessionId,
            EventReviewRequest.SaveDto saveDto, HttpSession session) {

        // 로그인 사용자 확인
        LoginIdentity identity = LoginSession.requireIdentity(session);

        // 기업 회원 후기 작성 제한
        if (identity.isCompany()) {
            throw new Exception403("일반 회원만 후기를 작성할 수 있습니다.");
        }

        // 일반 회원 세션 확인
        Object value = session.getAttribute(Define.SESSION_USER);
        if (!(value instanceof SessionUserDto sessionUserDto) || !Objects.equals(identity.getId(), sessionUserDto.getId())) {
            throw new Exception401("회원 계정으로 다시 로그인해주세요");
        }

        eventReviewService.save(
                eventSessionId,
                sessionUserDto.getId(),
                saveDto.getContent()
        );

        return "redirect:/event-review/" + eventSessionId;
    }

    // 후기 수정
    @PostMapping("/event-review/update/{id}")
    public String update(@PathVariable Long id,
                         EventReviewRequest.UpdateDto updateDto, HttpSession session) {

        // 로그인 사용자 확인
        LoginIdentity identity = LoginSession.requireIdentity(session);

        // 기업 회원 후기 수정 제한
        if (identity.isCompany()) {
            throw new Exception403("일반 회원만 후기를 수정할 수 있습니다.");
        }

        Object value = session.getAttribute(Define.SESSION_USER);
        if (!(value instanceof SessionUserDto sessionUserDto)
        || !Objects.equals(identity.getId(), sessionUserDto.getId())) {
            throw new Exception401("회원 계정으로 다시 로그인해주세요");
        }

        // 후기 수정
        Long eventSessionId = eventReviewService.update(id,
                sessionUserDto.getId(),
                updateDto.getContent());

        return "redirect:/event-review/" + eventSessionId;
    }

    // 후기 삭제
    @PostMapping("/event-review/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {

        // 로그인 사용자 확인
        LoginIdentity identity = LoginSession.requireIdentity(session);

        // 기업 회원 후기 삭제 제한
        if (identity.isCompany()) {
            throw new Exception403("일반 회원만 후기를 삭제할 수 있습니다.");
        }

        Object value = session.getAttribute(Define.SESSION_USER);
        if (!(value instanceof SessionUserDto sessionUserDto)
                || !Objects.equals(identity.getId(), sessionUserDto.getId())) {
            throw new Exception401("회원 계정으로 다시 로그인해주세요");
        }

        Long eventSessionId = eventReviewService.delete(id, sessionUserDto.getId());

        return "redirect:/event-review/" + eventSessionId;
    }

    // 좋아요 등록 및 취소
    @PostMapping("/event-review/like/{id}")
    public String toggleLike(@PathVariable Long id, HttpSession session) {

        LoginIdentity identity = LoginSession.requireIdentity(session);

        if (identity.isCompany()) {
            throw new Exception403("일반 회원만 좋아요를 누를 수 있습니다.");
        }

        Object value = session.getAttribute(Define.SESSION_USER);

        if (!(value instanceof SessionUserDto sessionUserDto) || !Objects.equals(identity.getId(), sessionUserDto.getId())) {
            throw new Exception401("회원 계정으로 다시 로그인해주세요.");
        }

        Long eventSessionId = eventReviewService.toggleLike(id, sessionUserDto.getId());

        return "redirect:/event-review/" + eventSessionId;
    }
}
