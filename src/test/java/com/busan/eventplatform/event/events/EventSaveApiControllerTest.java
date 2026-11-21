package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.eventCategory.EventCategoryService;
import com.busan.eventplatform.event.eventSession.EventSessionService;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.GlobalExceptionHandler;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.LoginViewAdvice;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.user.registration.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 DB 없이 요청 분기·입력 변환·JSON 예외 응답을 검증한다. */
class EventSaveApiControllerTest {
    private EventService eventService;
    private MockMvc mvc;
    private MockHttpSession companySession;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        EventController legacy = new EventController(eventService,
                mock(EventCategoryService.class), mock(EventSessionService.class),
                mock(RegistrationService.class));
        mvc = MockMvcBuilders.standaloneSetup(legacy, new EventSaveApiController(eventService))
                .setControllerAdvice(new EventSaveApiExceptionHandler(),
                        new GlobalExceptionHandler(new LoginViewAdvice()))
                .build();
        companySession = new MockHttpSession();
        companySession.setAttribute(LoginSession.IDENTITY,
                LoginIdentity.builder().id(1L).loginType(LoginType.COMPANY).build());
        companySession.setAttribute(LoginSession.COMPANY,
                SessionCompanyDto.builder().id(1L).displayName("테스트기업").build());
    }

    @Test
    void fetchSuccessReturnsJsonInsteadOfFollowingRedirect() throws Exception {
        mvc.perform(fetchRequest().session(companySession))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(EventSaveApiController.class))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.redirectUrl").value("/events"));
        verify(eventService).save(any(EventRequest.SaveDto.class), eq(1L), any());
    }

    @Test
    void normalFormStillUsesOriginalControllerAndRedirect() throws Exception {
        mvc.perform(validRequest(true).session(companySession))
                .andExpect(handler().handlerType(EventController.class))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"));
    }

    @Test
    void invalidInputReturnsJsonWithoutCallingSave() throws Exception {
        mvc.perform(fetchRequest().session(companySession)
                        .with(request -> { request.setParameter("title", " "); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("행사 제목은 필수 입니다."));
        verifyNoInteractions(eventService);
    }

    @Test
    void normalFormValidationStillReturnsOriginalErrorView() throws Exception {
        mvc.perform(validRequest(true).session(companySession)
                        .with(request -> { request.setParameter("title", " "); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"));
        verifyNoInteractions(eventService);
    }

    @Test
    void anonymousFetchReturns401Json() throws Exception {
        mvc.perform(fetchRequest()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(eventService);
    }

    @Test
    void personalAccountCannotSubmitAsCompany() throws Exception {
        companySession.setAttribute(LoginSession.IDENTITY,
                LoginIdentity.builder().id(1L).loginType(LoginType.USER).build());
        mvc.perform(fetchRequest().session(companySession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(eventService);
    }

    @Test
    void mismatchedSessionIdentityCannotSave() throws Exception {
        companySession.setAttribute(LoginSession.COMPANY,
                SessionCompanyDto.builder().id(2L).displayName("다른기업").build());
        mvc.perform(fetchRequest().session(companySession))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(eventService);
    }

    @Test
    void missingImageReturns400Json() throws Exception {
        mvc.perform(validRequest(false).session(companySession)
                        .header("X-Requested-With", "XMLHttpRequest").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("대표 이미지를 등록해주세요."));
        verifyNoInteractions(eventService);
    }

    @Test
    void malformedDateReturns400InsteadOfServerError() throws Exception {
        mvc.perform(fetchRequest().session(companySession)
                        .with(request -> { request.setParameter("startDate", "invalid-date"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("날짜·시간·숫자 입력 형식을 확인해주세요."));
        verifyNoInteractions(eventService);
    }

    @Test
    void serviceRejectionReturnsMessageAsJson() throws Exception {
        when(eventService.save(any(), eq(1L), any()))
                .thenThrow(new Exception403("승인된 기업만 신청할 수 있습니다."));
        mvc.perform(fetchRequest().session(companySession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("승인된 기업만 신청할 수 있습니다."));
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() throws Exception {
        when(eventService.save(any(), eq(1L), any()))
                .thenThrow(new IllegalStateException("sensitive-internal-details"));
        mvc.perform(fetchRequest().session(companySession))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(not(containsString("sensitive-internal-details"))));
    }

    private MockMultipartHttpServletRequestBuilder fetchRequest() {
        return validRequest(true).header("X-Requested-With", "XMLHttpRequest")
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockMultipartHttpServletRequestBuilder validRequest(boolean withImage) {
        MockMultipartHttpServletRequestBuilder request = multipart("/events/save");
        if (withImage) {
            request.file(new MockMultipartFile("file", "poster.jpg", "image/jpeg", new byte[]{1}));
        }
        return request.param("title", "행사").param("summary", "소개")
                .param("description", "설명").param("address", "부산").param("postalCode", "48000")
                .param("categoryId", "1").param("hasSessions", "false")
                .param("pricingType", "FREE").param("startDate", "2099-06-20")
                .param("endDate", "2099-06-20").param("applyStartAt", "2099-06-01T10:00")
                .param("applyEndAt", "2099-06-10T12:00").param("sessions[0].capacity", "10")
                .param("sessions[0].startsAt", "2099-06-20T10:00")
                .param("sessions[0].endsAt", "2099-06-20T12:00");
    }
}
