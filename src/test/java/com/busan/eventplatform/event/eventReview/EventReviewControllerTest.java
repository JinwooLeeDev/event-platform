
package com.busan.eventplatform.event.eventReview;

import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.Role;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventReviewControllerTest {

    private EventReviewService eventReviewService;
    private EventReviewController eventReviewController;

    private final Long SESSION_ID = 1L;
    private final Long USER_ID = 10L;
    private final Long REVIEW_ID = 100L;

    @BeforeEach
    void setUp() {
        eventReviewService = mock(EventReviewService.class);
        eventReviewController = new EventReviewController(eventReviewService);
    }

    // 일반 회원 로그인 세션 생성
    private MockHttpSession createUserSession() {
        MockHttpSession session = new MockHttpSession();

        LoginIdentity identity = LoginIdentity.builder()
                .id(USER_ID)
                .loginType(LoginType.USER)
                .build();

        SessionUserDto userDto = SessionUserDto.builder()
                .id(USER_ID)
                .displayName("테스트회원")
                .role(Role.MEMBER)
                .preferredLocale("ko")
                .build();

        session.setAttribute(LoginSession.IDENTITY, identity);
        session.setAttribute(Define.SESSION_USER, userDto);

        return session;
    }

    // 기업 회원 로그인 세션 생성
    private MockHttpSession createCompanySession() {
        MockHttpSession session = new MockHttpSession();

        LoginIdentity identity = LoginIdentity.builder()
                .id(USER_ID)
                .loginType(LoginType.COMPANY)
                .build();

        session.setAttribute(LoginSession.IDENTITY, identity);

        return session;
    }

    // 로그인하지 않은 경우
    @Test
    void 로그인하지_않으면_후기작성_실패() {
        EventReviewRequest.SaveDto dto = new EventReviewRequest.SaveDto();
        dto.setContent("후기");

        assertThrows(
                Exception401.class,
                () -> eventReviewController.save(SESSION_ID, dto, null)
        );

        verifyNoInteractions(eventReviewService);
    }

    // 후기 목록 조회
    @Test
    void 후기목록_조회() {
        ExtendedModelMap model = new ExtendedModelMap();

        String result = eventReviewController.list(
                SESSION_ID,
                "latest",
                model
        );

        assertEquals("event/review-list", result);
        assertEquals("latest", model.get("sort"));
        assertNotNull(model.get("reviewList"));

        verify(eventReviewService)
                .findAllByEventSessionId(SESSION_ID, "latest");
    }

    // 좋아요순 후기 목록 조회
    @Test
    void 좋아요순_후기목록_조회() {
        ExtendedModelMap model = new ExtendedModelMap();

        String result = eventReviewController.list(
                SESSION_ID,
                "like",
                model
        );

        assertEquals("event/review-list", result);
        assertEquals("like", model.get("sort"));

        verify(eventReviewService)
                .findAllByEventSessionId(SESSION_ID, "like");
    }

    // 후기 작성 성공
    @Test
    void 후기작성_성공() {
        EventReviewRequest.SaveDto dto = new EventReviewRequest.SaveDto();
        dto.setContent("좋은 행사였습니다.");

        HttpSession session = createUserSession();

        String result = eventReviewController.save(
                SESSION_ID,
                dto,
                session
        );

        assertEquals("redirect:/event-review/" + SESSION_ID, result);

        verify(eventReviewService)
                .save(SESSION_ID, USER_ID, "좋은 행사였습니다.");
    }

    // 기업 회원 후기 작성 차단
    @Test
    void 기업회원은_후기작성할_수_없음() {
        EventReviewRequest.SaveDto dto = new EventReviewRequest.SaveDto();
        dto.setContent("후기");

        HttpSession session = createCompanySession();

        assertThrows(
                Exception403.class,
                () -> eventReviewController.save(SESSION_ID, dto, session)
        );

        verifyNoInteractions(eventReviewService);
    }

    // 일반 회원 세션이 없는 경우
    @Test
    void 일반회원_세션이_없으면_후기작성_실패() {
        MockHttpSession session = new MockHttpSession();

        LoginIdentity identity = LoginIdentity.builder()
                .id(USER_ID)
                .loginType(LoginType.USER)
                .build();

        session.setAttribute(LoginSession.IDENTITY, identity);

        EventReviewRequest.SaveDto dto = new EventReviewRequest.SaveDto();
        dto.setContent("후기");

        assertThrows(
                Exception401.class,
                () -> eventReviewController.save(SESSION_ID, dto, session)
        );

        verifyNoInteractions(eventReviewService);
    }

    // 후기 수정 성공
    @Test
    void 후기수정_성공() {
        EventReviewRequest.UpdateDto dto = new EventReviewRequest.UpdateDto();
        dto.setContent("수정된 후기");

        when(eventReviewService.update(REVIEW_ID, USER_ID, "수정된 후기"))
                .thenReturn(SESSION_ID);

        String result = eventReviewController.update(
                REVIEW_ID,
                dto,
                createUserSession()
        );

        assertEquals("redirect:/event-review/" + SESSION_ID, result);

        verify(eventReviewService)
                .update(REVIEW_ID, USER_ID, "수정된 후기");
    }

    // 기업 회원 후기 수정 차단
    @Test
    void 기업회원은_후기를_수정할_수_없음() {
        EventReviewRequest.UpdateDto dto = new EventReviewRequest.UpdateDto();
        dto.setContent("수정");

        assertThrows(
                Exception403.class,
                () -> eventReviewController.update(
                        REVIEW_ID,
                        dto,
                        createCompanySession()
                )
        );

        verifyNoInteractions(eventReviewService);
    }

    // 후기 삭제 성공
    @Test
    void 후기삭제_성공() {
        when(eventReviewService.delete(REVIEW_ID, USER_ID))
                .thenReturn(SESSION_ID);

        String result = eventReviewController.delete(
                REVIEW_ID,
                createUserSession()
        );

        assertEquals("redirect:/event-review/" + SESSION_ID, result);

        verify(eventReviewService).delete(REVIEW_ID, USER_ID);
    }

    // 기업 회원 후기 삭제 차단
    @Test
    void 기업회원은_후기를_삭제할_수_없음() {
        assertThrows(
                Exception403.class,
                () -> eventReviewController.delete(
                        REVIEW_ID,
                        createCompanySession()
                )
        );

        verifyNoInteractions(eventReviewService);
    }

    // 좋아요 등록 성공
    @Test
    void 좋아요_등록_성공() {
        when(eventReviewService.toggleLike(REVIEW_ID, USER_ID))
                .thenReturn(SESSION_ID);

        String result = eventReviewController.toggleLike(
                REVIEW_ID,
                createUserSession()
        );

        assertEquals("redirect:/event-review/" + SESSION_ID, result);

        verify(eventReviewService).toggleLike(REVIEW_ID, USER_ID);
    }

    // 기업 회원 좋아요 차단
    @Test
    void 기업회원은_좋아요를_누를_수_없음() {
        assertThrows(
                Exception403.class,
                () -> eventReviewController.toggleLike(
                        REVIEW_ID,
                        createCompanySession()
                )
        );

        verifyNoInteractions(eventReviewService);
    }
}
