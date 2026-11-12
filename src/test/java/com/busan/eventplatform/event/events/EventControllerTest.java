
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventSessionService eventSessionService;

    private EventController eventController;

    @BeforeEach
    void setUp() {
        eventController = new EventController(eventService, eventSessionService);
    }

    // 기업 로그인 세션 생성
    private HttpSession companySession(Long companyId) {
        HttpSession session = mock(HttpSession.class);
        SessionCompanyDto companyDto = mock(SessionCompanyDto.class);

        when(companyDto.getId()).thenReturn(companyId);
        when(session.getAttribute(Define.SESSION_COMPANY)).thenReturn(companyDto);

        return session;
    }

    // 개인 로그인 세션 생성
    private HttpSession userSession(Long userId) {
        HttpSession session = mock(HttpSession.class);
        SessionUserDto userDto = mock(SessionUserDto.class);

        when(userDto.getId()).thenReturn(userId);
        when(session.getAttribute(Define.SESSION_USER)).thenReturn(userDto);

        return session;
    }

    // 개인 + 기업 로그인 세션 생성
    private HttpSession bothSession(Long companyId, Long userId) {
        HttpSession session = mock(HttpSession.class);

        SessionCompanyDto companyDto = mock(SessionCompanyDto.class);
        SessionUserDto userDto = mock(SessionUserDto.class);

        when(companyDto.getId()).thenReturn(companyId);
        when(userDto.getId()).thenReturn(userId);

        when(session.getAttribute(Define.SESSION_COMPANY)).thenReturn(companyDto);
        when(session.getAttribute(Define.SESSION_USER)).thenReturn(userDto);

        return session;
    }

    // 1. 행사 목록 조회
    @Test
    void list_test() {
        // given
        int page = 1;
        int size = 10;
        String keyword = "축제";
        Long userId = 1L;

        HttpSession session = userSession(userId);
        Model model = new ExtendedModelMap();

        // when
        String viewName = eventController.list(page, size, keyword, model, session);

        // then
        assertThat(viewName).isEqualTo("event/list");
        assertThat(model.getAttribute("keyword")).isEqualTo(keyword);
        assertThat(model.getAttribute("isSearch")).isEqualTo(true);

        verify(eventService).findAllPage(page, size, keyword, userId);
    }

    // 2. 비로그인 행사 목록 조회
    @Test
    void list_guest_test() {
        // given
        HttpSession session = mock(HttpSession.class);
        Model model = new ExtendedModelMap();

        // when
        String viewName = eventController.list(1, 10, "", model, session);

        // then
        assertThat(viewName).isEqualTo("event/list");
        assertThat(model.getAttribute("isSearch")).isEqualTo(false);

        verify(eventService).findAllPage(1, 10, "", null);
    }

    // 3. 행사 상세 조회
    @Test
    void detail_test() {
        // given
        Long eventId = 1L;
        Long companyId = 10L;
        Long userId = 20L;

        HttpSession session = bothSession(companyId, userId);
        Model model = new ExtendedModelMap();

        EventResponse.DetailDto detailDto = mock(EventResponse.DetailDto.class);

        when(eventService.findById(eventId, companyId, userId))
                .thenReturn(detailDto);

        // when
        String viewName = eventController.detail(eventId, model, session);

        // then
        assertThat(viewName).isEqualTo("event/detail");
        assertThat(model.getAttribute("event")).isEqualTo(detailDto);

        verify(eventService).findById(eventId, companyId, userId);
        verify(eventSessionService).findByEventId(eventId);
    }

    // 4. 행사 등록 화면
    @Test
    void saveForm_test() {
        // when
        String viewName = eventController.saveForm();

        // then
        assertThat(viewName).isEqualTo("event/save-form");
    }

    // 5. 행사 등록
    @Test
    void save_test() {
        // given
        EventRequest.SaveDto saveDto = mock(EventRequest.SaveDto.class);
        HttpSession session = companySession(10L);

        // when
        String viewName = eventController.save(saveDto, session);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(saveDto).validate();
        verify(eventService).save(saveDto, 10L);
    }

    // 6. 기업이 등록한 행사 전체 조회
    @Test
    void findAllByCompanyId_test() {
        // given
        HttpSession session = companySession(10L);
        Model model = new ExtendedModelMap();

        // when
        String viewName = eventController.findAllByCompanyId(session, model);

        // then
        assertThat(viewName).isEqualTo("company/event-list");

        verify(eventService).findAllByCompanyId(10L);
    }

    // 7. 기업이 보완 요청받은 행사 조회
    @Test
    void findAllFeedback_test() {
        // given
        HttpSession session = companySession(10L);
        Model model = new ExtendedModelMap();

        // when
        String viewName = eventController.findAllFeedback(session, model);

        // then
        assertThat(viewName).isEqualTo("company/event-feedback-list");

        verify(eventService).findAllFeedback(10L);
    }

    // 8. 행사 수정 화면
    @Test
    void updateForm_test() {
        // given
        Long eventId = 1L;
        HttpSession session = companySession(10L);
        Model model = new ExtendedModelMap();

        EventResponse.UpdateFormDto updateFormDto =
                mock(EventResponse.UpdateFormDto.class);

        when(eventService.update(eventId, 10L))
                .thenReturn(updateFormDto);

        // when
        String viewName = eventController.updateForm(eventId, session, model);

        // then
        assertThat(viewName).isEqualTo("event/update-form");
        assertThat(model.getAttribute("event")).isEqualTo(updateFormDto);

        verify(eventService).update(eventId, 10L);
    }

    // 9. 행사 수정
    @Test
    void update_test() {
        // given
        Long eventId = 1L;
        EventRequest.UpdateDto updateDto = mock(EventRequest.UpdateDto.class);
        HttpSession session = companySession(10L);

        // when
        String viewName = eventController.update(eventId, updateDto, session);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(updateDto).validate();
        verify(eventService).update(eventId, 10L, updateDto);
    }

    // 10. 보완 요청된 행사 재제출
    @Test
    void resubmit_test() {
        // given
        Long eventId = 1L;
        HttpSession session = companySession(10L);

        // when
        String viewName = eventController.resubmit(eventId, session);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(eventService).resubmit(eventId, 10L);
    }

    // 11. 기업 행사 삭제
    @Test
    void delete_test() {
        // given
        Long eventId = 1L;
        HttpSession session = companySession(10L);

        // when
        String viewName = eventController.delete(eventId, session);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(eventService).delete(eventId, 10L);
    }

    // 12. 관리자 행사 승인
    @Test
    void approve_test() {
        // given
        Long eventId = 1L;

        // when
        String viewName = eventController.approve(eventId);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(eventService).approve(eventId);
    }

    // 13. 관리자 보완 요청
    @Test
    void requestFeedback_test() {
        // given
        Long eventId = 1L;
        String feedbackMessage = "행사 상세 내용을 보완해 주세요.";

        // when
        String viewName = eventController.requestFeedback(eventId, feedbackMessage);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(eventService).requestFeedback(eventId, feedbackMessage);
    }

    // 14. 관리자 행사 취소
    @Test
    void cancel_test() {
        // given
        Long eventId = 1L;
        EventRequest.CancelDto cancelDto = mock(EventRequest.CancelDto.class);

        // when
        String viewName = eventController.cancel(eventId, cancelDto);

        // then
        assertThat(viewName).isEqualTo("redirect:/events");

        verify(cancelDto).validate();
        verify(eventService).cancel(eventId, cancelDto);
    }
}
