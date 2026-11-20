package com.busan.eventplatform.event;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.eventCategory.EventCategory;
import com.busan.eventplatform.event.eventFile.EventFileService;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.login.*;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("h2")
@SpringBootTest(properties={"spring.config.import=","spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:event-save-period;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=",
        "app.mail.from=test@example.test","spring.mail.host=localhost","spring.mail.username=test","spring.mail.password=test",
        "app.business-verification.service-key=test",
        "portone.naver-store-id=test","portone.naver-channel-key=test","portone.naver-secret-key=test",
        "portone.kakao-store-id=test","portone.kakao-channel-key=test","portone.kakao-secret-key=test",
        "naver-map.client-id=test","naver-map.client-secret=test"})
@Transactional
class EventSavePeriodTest {
    @Autowired EntityManager em;
    @Autowired EventSessionRepository sessions;
    @Autowired WebApplicationContext context;
    @MockitoBean EventFileService files; // 이미지 저장은 이번 회귀 검증의 범위가 아닙니다.
    MockMvc mvc;
    MockHttpSession login;
    Long categoryId;

    @BeforeEach
    void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).build();
        var company=Company.builder().name("기간 테스트기업").email("period@example.test").password("test")
                .businessNumber("0000000001").representativeName("대표").openedOn(LocalDate.of(2020,1,1))
                .address("부산").contactPersonName("담당").contactPersonPhone("010-0000-0001").status(CompanyStatus.APPROVED).build();
        em.persist(company);
        var category=EventCategory.builder().name("체험").build(); em.persist(category); categoryId=category.getId();
        login=new MockHttpSession();
        login.setAttribute(LoginSession.IDENTITY,LoginIdentity.builder().id(company.getId()).loginType(LoginType.COMPANY).build());
        login.setAttribute(LoginSession.COMPANY,SessionCompanyDto.builder().id(company.getId()).displayName("기업").build());
    }

    @Test
    void defaultSessionKeepsEventDatesAndSavesSeparateApplicationPeriod() throws Exception {
        mvc.perform(noSessionForm()).andExpect(status().is3xxRedirection());
        var saved=savedSession();
        assertEquals(LocalDateTime.parse("2030-12-20T09:30"),saved.getStartsAt());
        assertEquals(LocalDateTime.parse("2030-12-21T18:15"),saved.getEndsAt());
        assertEquals(17, saved.getCapacity());
        assertEquals(LocalDateTime.parse("2030-12-01T09:00"),saved.getApplyStartAt());
        assertEquals(LocalDateTime.parse("2030-12-18T18:00"),saved.getApplyEndAt());
    }

    @Test
    void noSessionRejectsMissingCapacityTimeAndReversedTime() throws Exception {
        for (String field : new String[]{"sessions[0].capacity", "sessions[0].startsAt", "sessions[0].endsAt"}) {
            mvc.perform(noSessionForm().with(request -> { request.setParameter(field, ""); return request; }))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(noSessionForm().with(request -> { request.setParameter("sessions[0].capacity", "0"); return request; }))
                .andExpect(status().isBadRequest());
        mvc.perform(noSessionForm().with(request -> { request.setParameter("sessions[0].endsAt", "2030-12-20T08:00"); return request; }))
                .andExpect(status().isBadRequest());
        mvc.perform(noSessionForm().with(request -> {
            request.removeParameter("sessions[0].capacity"); request.removeParameter("sessions[0].startsAt");
            request.removeParameter("sessions[0].endsAt"); return request;
        })).andExpect(status().isBadRequest());
    }

    @Test
    void nestedSessionFormSavesOwnApplicationDatesWithoutTopLevelDates() throws Exception {
        mvc.perform(nestedForm()).andExpect(status().is3xxRedirection());
        var saved=savedSession();
        assertEquals(LocalDateTime.parse("2030-12-20T14:00"),saved.getStartsAt());
        assertEquals(LocalDateTime.parse("2030-12-01T09:00"),saved.getApplyStartAt());
        assertEquals(LocalDateTime.parse("2030-12-18T18:00"),saved.getApplyEndAt());
        assertEquals(10,saved.getCapacity());
    }

    @Test
    void savedSessionPeriodsRenderOnHomeListsAndDetail() throws Exception {
        mvc.perform(nestedForm()).andExpect(status().is3xxRedirection());
        var saved = savedSession();
        saved.getEvent().setStatus(com.busan.eventplatform.event.events.EventStatus.PUBLISHED);
        em.flush();
        for (String url : new String[]{"/", "/events/list", "/events"}) {
            mvc.perform(get(url).session(login)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("오후 회차")))
                    .andExpect(content().string(containsString("2030.12.01 09:00")))
                    .andExpect(content().string(containsString("2030.12.18 18:00")));
        }
        mvc.perform(get("/event/" + saved.getEvent().getId()).session(login))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-apply-start=\"2030-12-01T09:00\"")))
                .andExpect(content().string(containsString("data-apply-end=\"2030-12-18T18:00\"")));
    }

    @Test
    void missingOrReversedNestedApplicationPeriodIsRejected() throws Exception {
        var missing=nestedForm(); missing.param("sessions[0].applyStartAt", "");
        // param()은 값을 추가하므로 해당 파라미터를 직접 대체합니다.
        missing.with(request -> { request.setParameter("sessions[0].applyStartAt", ""); return request; });
        mvc.perform(missing).andExpect(status().isBadRequest());
        var reversed=nestedForm().with(request -> { request.setParameter("sessions[0].applyEndAt","2030-11-01T18:00"); return request; });
        mvc.perform(reversed).andExpect(status().isBadRequest());
        assertEquals(0L,em.createQuery("select count(e) from Event e",Long.class).getSingleResult());
    }

    private MockMultipartHttpServletRequestBuilder form() {
        return multipart("/events/save").file(new MockMultipartFile("file","poster.png","image/png",new byte[]{1}))
                .session(login).param("categoryId",categoryId.toString()).param("title","행사").param("summary","소개")
                .param("description","상세 소개").param("address","부산").param("postalCode","48058");
    }
    private MockMultipartHttpServletRequestBuilder noSessionForm() {
        return form().param("hasSessions","false").param("startDate","2030-12-20").param("endDate","2030-12-21")
                .param("applyStartAt","2030-12-01T09:00").param("applyEndAt","2030-12-18T18:00").param("pricingType","FREE")
                .param("sessions[0].startsAt","2030-12-20T09:30").param("sessions[0].endsAt","2030-12-21T18:15")
                .param("sessions[0].capacity","17");
    }
    private MockMultipartHttpServletRequestBuilder nestedForm() {
        var request = form().param("hasSessions","true");
        for (int index = 0; index < 2; index++) {
            String prefix = "sessions[" + index + "].";
            request.param(prefix+"name","오후 회차").param(prefix+"startsAt","2030-12-20T14:00")
                    .param(prefix+"endsAt","2030-12-20T16:00").param(prefix+"applyStartAt","2030-12-01T09:00")
                    .param(prefix+"applyEndAt","2030-12-18T18:00").param(prefix+"capacity","10")
                    .param(prefix+"pricingType","FREE").param(prefix+"minimumParticipants","1")
                    .param(prefix+"minimumCheckAt","2030-12-20T14:00");
        }
        return request;
    }
    private EventSession savedSession() {
        em.flush(); em.clear();
        var event=em.createQuery("from Event",Event.class).getSingleResult();
        return sessions.findByEventId(event.getId()).getFirst();
    }
}
