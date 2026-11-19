
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_category.EventCategoryService;
import com.busan.eventplatform.event.eventSession.EventSessionService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.registration.RegistrationService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventCategoryService eventCategoryService;

    @Mock
    private EventSessionService eventSessionService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private EventController eventController;


    // 1. 보완 요청 목록 - 비로그인 사용자 접근 차단
    @Test
    void findAllFeedback_fail_whenNotLoggedIn() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(null);

        assertThrows(Exception403.class, () ->
                eventController.findAllFeedback(session, model)
        );

        verifyNoInteractions(eventService);
    }


    // 2. 보완 요청 목록 - 개인 회원 접근 차단
    @Test
    void findAllFeedback_fail_whenPersonalUser() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(mock(SessionUserDto.class));

        assertThrows(Exception403.class, () ->
                eventController.findAllFeedback(session, model)
        );

        verifyNoInteractions(eventService);
    }


    // 3. 보완 요청 목록 - 기업 회원 정상 접근
    @Test
    void findAllFeedback_success_whenCompany() {
        SessionCompanyDto company = mock(SessionCompanyDto.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(company);
        when(company.getId()).thenReturn(10L);

        String view = eventController.findAllFeedback(session, model);

        assertEquals("company/event-feedback-list", view);

        verify(eventService).findAllFeedback(10L);
    }


    // 4. 행사 수정 화면 - 비로그인 사용자 접근 차단
    @Test
    void updateForm_fail_whenNotLoggedIn() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(null);

        assertThrows(Exception403.class, () ->
                eventController.updateForm(1L, session, model)
        );

        verifyNoInteractions(eventService);
    }


    // 5. 행사 수정 화면 - 개인 회원 접근 차단
    @Test
    void updateForm_fail_whenPersonalUser() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(mock(SessionUserDto.class));

        assertThrows(Exception403.class, () ->
                eventController.updateForm(1L, session, model)
        );

        verifyNoInteractions(eventService);
    }


    // 6. 행사 수정 - 비로그인 사용자 접근 차단
    @Test
    void update_fail_whenNotLoggedIn() {
        EventRequest.UpdateDto updateDto =
                mock(EventRequest.UpdateDto.class);

        doNothing().when(updateDto).validate();

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(null);

        RedirectAttributes redirect = mock(RedirectAttributes.class);

        assertThrows(Exception403.class, () ->
                eventController.update(1L, updateDto, session, null, redirect)
        );

        verifyNoInteractions(eventService);
    }


    // 7. 행사 수정 - 개인 회원 접근 차단
    @Test
    void update_fail_whenPersonalUser() {
        EventRequest.UpdateDto updateDto =
                mock(EventRequest.UpdateDto.class);

        doNothing().when(updateDto).validate();

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(mock(SessionUserDto.class));

        RedirectAttributes redirect = mock(RedirectAttributes.class);

        assertThrows(Exception403.class, () ->
                eventController.update(1L, updateDto, session, null, redirect)
        );

        verifyNoInteractions(eventService);
    }


    // 8. 행사 수정 - 기업 회원 정상 접근
    @Test
    void update_success_whenCompany() {
        EventRequest.UpdateDto updateDto =
                mock(EventRequest.UpdateDto.class);

        doNothing().when(updateDto).validate();

        SessionCompanyDto company = mock(SessionCompanyDto.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(company);
        when(company.getId()).thenReturn(10L);

        RedirectAttributes redirect = mock(RedirectAttributes.class);

        String view = eventController.update(
                1L, updateDto, session, null, redirect
        );

        assertEquals("redirect:/events", view);

        verify(eventService).updateAndResubmit(
                1L, 10L, updateDto, null
        );
    }


    // 9. 행사 재제출 - 비로그인 사용자 접근 차단
    @Test
    void resubmit_fail_whenNotLoggedIn() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(null);

        assertThrows(Exception403.class, () ->
                eventController.resubmit(1L, session)
        );

        verifyNoInteractions(eventService);
    }


    // 10. 행사 재제출 - 개인 회원 접근 차단
    @Test
    void resubmit_fail_whenPersonalUser() {
        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(mock(SessionUserDto.class));

        assertThrows(Exception403.class, () ->
                eventController.resubmit(1L, session)
        );

        verifyNoInteractions(eventService);
    }


    // 11. 행사 재제출 - 기업 회원 정상 접근
    @Test
    void resubmit_success_whenCompany() {
        SessionCompanyDto company = mock(SessionCompanyDto.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(company);
        when(company.getId()).thenReturn(10L);

        String view = eventController.resubmit(1L, session);

        assertEquals("redirect:/events", view);

        verify(eventService).resubmit(1L, 10L);
    }


    @Test
    void save_fail_whenNotLoggedIn() {
        EventRequest.SaveDto saveDto = mock(EventRequest.SaveDto.class);
        MultipartFile file = mock(MultipartFile.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(null);

        assertThrows(Exception403.class, () ->
                eventController.save(saveDto, file, session)
        );

        verifyNoInteractions(eventService);
    }

    @Test
    void save_fail_whenFileIsEmpty() {
        EventRequest.SaveDto saveDto = mock(EventRequest.SaveDto.class);
        MultipartFile file = mock(MultipartFile.class);
        SessionCompanyDto company = mock(SessionCompanyDto.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(company);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(Exception400.class, () ->
                eventController.save(saveDto, file, session)
        );

        verifyNoInteractions(eventService);
    }

    @Test
    void save_success_whenCompanyAndFileExists() {
        EventRequest.SaveDto saveDto = mock(EventRequest.SaveDto.class);
        MultipartFile file = mock(MultipartFile.class);
        SessionCompanyDto company = mock(SessionCompanyDto.class);

        when(session.getAttribute(Define.SESSION_COMPANY))
                .thenReturn(company);
        when(company.getId()).thenReturn(10L);
        when(file.isEmpty()).thenReturn(false);

        String view = eventController.save(saveDto, file, session);

        assertEquals("redirect:/", view);

        verify(eventService).save(saveDto, 10L, file);
    }

}
