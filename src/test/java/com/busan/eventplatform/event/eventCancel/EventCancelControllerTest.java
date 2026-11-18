package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EventCancelControllerTest {

    private MockMvc mockMvc;

    private EventCancelService eventCancelService;

    private SessionCompanyDto sessionCompanyDto;

    @BeforeEach
    void setUp() {
        eventCancelService = mock(EventCancelService.class);
        sessionCompanyDto = mock(SessionCompanyDto.class);

        EventCancelController controller =
                new EventCancelController(eventCancelService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void 취소신청_페이지_접근() throws Exception {
        MockHttpSession session = companySession();
        when(eventCancelService.context(1L, 10L)).thenReturn(java.util.Map.of("canRequestCancel", true));
        // when & then
        mockMvc.perform(get("/event-cancels/save/10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("event_cancel/save-form"))
                .andExpect(model().attribute("sessionId", 10L));
    }

    @Test
    void 취소신청_제출() throws Exception {
        // given
        MockHttpSession session = companySession();

        when(sessionCompanyDto.getId()).thenReturn(1L);

        session.setAttribute(
                Define.SESSION_COMPANY,
                sessionCompanyDto
        );

        // when & then
        mockMvc.perform(post("/event-cancels/save/10")
                        .session(session)
                        .param("reason", "행사 운영이 어려워졌습니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/event-sessions/10"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(eventCancelService).save(
                1L,
                10L,
                "행사 운영이 어려워졌습니다."
        );
    }

    private MockHttpSession companySession() {
        MockHttpSession session = new MockHttpSession();
        when(sessionCompanyDto.getId()).thenReturn(1L);
        session.setAttribute(Define.SESSION_COMPANY, sessionCompanyDto);
        session.setAttribute(com.busan.eventplatform.login.LoginSession.IDENTITY,
                com.busan.eventplatform.login.LoginIdentity.builder().id(1L)
                        .loginType(com.busan.eventplatform.login.LoginType.COMPANY).build());
        return session;
    }

    @Test void unauthenticatedFormCannotExposeSession() {
        var controller = new EventCancelController(eventCancelService);
        org.junit.jupiter.api.Assertions.assertThrows(
                com.busan.eventplatform.global.error.exception.Exception401.class,
                () -> controller.form(10L, new MockHttpSession(), new org.springframework.ui.ExtendedModelMap()));
        verifyNoInteractions(eventCancelService);
    }
}
