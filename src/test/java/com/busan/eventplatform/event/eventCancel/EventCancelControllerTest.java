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
        // when & then
        mockMvc.perform(get("/event-cancels/save/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("event_cancel/save-form"))
                .andExpect(model().attribute("sessionId", 10L));
    }

    @Test
    void 취소신청_제출() throws Exception {
        // given
        MockHttpSession session = new MockHttpSession();

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
                .andExpect(redirectedUrl("/events"));

        verify(eventCancelService).save(
                1L,
                10L,
                "행사 운영이 어려워졌습니다."
        );
    }
}