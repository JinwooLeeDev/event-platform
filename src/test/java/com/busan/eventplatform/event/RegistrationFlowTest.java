package com.busan.eventplatform.event;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.event_category.EventCategory;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.registration.RegistrationPurchaseService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("h2")
@SpringBootTest(properties = {
        "spring.config.import=", "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:registration-flow;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.mail.from=test@example.test", "spring.mail.host=localhost",
        "spring.mail.username=test", "spring.mail.password=test",
        "app.business-verification.service-key=test",
        "portone.store-id=test", "portone.channel-key=test", "portone.secret-key=test",
        "naver-map.client-id=test", "naver-map.client-secret=test"
})
class RegistrationFlowTest {
    private static final AtomicInteger sequence = new AtomicInteger();
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RegistrationPurchaseService registrationPurchaseService;
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void selectedSessionRendersChecksOutAndSavesApplicationWithPoints() throws Exception {
        Fixture f = fixture(500, 2000, 2);
        var login = login(f.userId);
        mvc.perform(get("/event/" + f.eventId).session(login))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"sessionId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("detail-button reg-apply-btn")));
        mvc.perform(get("/api/registrations/check/" + f.sessionId).session(login))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canApply").value(true))
                .andExpect(jsonPath("$.enoughPoint").value(true)).andExpect(jsonPath("$.fee").value(500));
        mvc.perform(get("/event/purchase/" + f.sessionId).session(login))
                .andExpect(status().isOk()).andExpect(view().name("point/purchase"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/event/purchase/"+f.sessionId+"\"")));
        mvc.perform(post("/event/purchase/" + f.sessionId).session(login))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/mypage"));
        assertEquals(1500, balance(f.userId));
        assertEquals(1, rows("registrations", f.sessionId));
        assertEquals(1, rows("purchase_tb", f.sessionId));
        assertEquals("CONFIRMED", jdbc.queryForObject("select status from registrations where event_session_id=?",String.class,f.sessionId));
        assertEquals(1, occupied(f.sessionId));
        assertEquals(1, jdbc.queryForObject("select count(*) from registrations r join purchase_tb p on p.id=r.purchase_id where r.event_session_id=? and p.user_id=r.user_id and p.price=500",Integer.class,f.sessionId));
        mvc.perform(post("/event/purchase/" + f.sessionId).session(login)).andExpect(status().isBadRequest());
        assertEquals(1500, balance(f.userId));
        assertEquals(1, rows("purchase_tb", f.sessionId));
    }

    @Test
    void freeFullSessionCreatesWaitingApplicationWithoutExtraSeatOrPoints() throws Exception {
        Fixture f = fixture(0, 2000, 1);
        registrationPurchaseService.applyWithPayment(f.sessionId, identity(f.userId));
        Long waitingUser = new TransactionTemplate(transactionManager).execute(s -> createUser(1000).getId());
        var login = login(waitingUser);
        mvc.perform(get("/api/registrations/check/" + f.sessionId).session(login))
                .andExpect(jsonPath("$.canApply").value(true)).andExpect(jsonPath("$.waitlist").value(true));
        mvc.perform(post("/event/purchase/" + f.sessionId).session(login)).andExpect(status().is3xxRedirection());
        assertEquals("WAITING", jdbc.queryForObject("select status from registrations where event_session_id=? and user_id=?",String.class,f.sessionId,waitingUser));
        assertEquals(1000,balance(waitingUser));
        assertEquals(1,occupied(f.sessionId));
        assertEquals(2,rows("registrations",f.sessionId));
        assertEquals(2,rows("purchase_tb",f.sessionId));
    }

    @Test
    void insufficientPointsDoesNotSavePurchaseOrApplication() throws Exception {
        Fixture f = fixture(3000,1000,2);
        mvc.perform(get("/api/registrations/check/"+f.sessionId).session(login(f.userId)))
                .andExpect(jsonPath("$.enoughPoint").value(false));
        mvc.perform(post("/event/purchase/"+f.sessionId).session(login(f.userId))).andExpect(status().isBadRequest());
        assertEquals(1000,balance(f.userId));
        assertEquals(0,rows("purchase_tb",f.sessionId));
        assertEquals(0,rows("registrations",f.sessionId));
        assertEquals(0,occupied(f.sessionId));
    }

    @Test
    void anonymousCompanyAndCancelledSessionCannotApply() throws Exception {
        Fixture f = fixture(500,2000,2);
        mvc.perform(get("/api/registrations/check/"+f.sessionId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/event/purchase/"+f.sessionId)).andExpect(status().isUnauthorized());
        var corporate = new MockHttpSession();
        corporate.setAttribute(LoginSession.IDENTITY,LoginIdentity.builder().id(1L).loginType(LoginType.COMPANY).build());
        mvc.perform(get("/api/registrations/check/"+f.sessionId).session(corporate)).andExpect(status().isForbidden());
        mvc.perform(post("/event/purchase/"+f.sessionId).session(corporate)).andExpect(status().isForbidden());
        jdbc.update("update event_sessions set status='CANCELLED' where id=?",f.sessionId);
        mvc.perform(get("/api/registrations/check/"+f.sessionId).session(login(f.userId)))
                .andExpect(jsonPath("$.canApply").value(false));
        mvc.perform(post("/event/purchase/"+f.sessionId).session(login(f.userId))).andExpect(status().isBadRequest());
        assertEquals(2000,balance(f.userId));
        assertEquals(0,rows("registrations",f.sessionId));
        assertEquals(0,rows("purchase_tb",f.sessionId));
    }

    @Test
    void applicationSaveFailureRollsBackPointDeductionAndPurchase() {
        Fixture f = fixture(500,2000,2);
        // 해당 테스트 회차의 신청 저장만 실패시켜 결제 이후 롤백을 검증합니다.
        jdbc.execute("alter table registrations add constraint registration_flow_failure check (event_session_id <> " + f.sessionId + ")");
        try {
            assertThrows(RuntimeException.class, () -> registrationPurchaseService.applyWithPayment(f.sessionId,identity(f.userId)));
            assertEquals(2000,balance(f.userId));
            assertEquals(0,rows("purchase_tb",f.sessionId));
            assertEquals(0,rows("registrations",f.sessionId));
            assertEquals(0,occupied(f.sessionId));
        } finally {
            jdbc.execute("alter table registrations drop constraint registration_flow_failure");
        }
    }

    private Fixture fixture(int fee,int points,int capacity) {
        return new TransactionTemplate(transactionManager).execute(s -> {
            int n = sequence.incrementAndGet();
            var company=Company.builder().name("신청테스트기업"+n).email("flow-company"+n+"@example.test")
                    .password("test").businessNumber(String.format("%010d",n)).representativeName("대표")
                    .openedOn(LocalDate.of(2020,1,1)).address("부산").businessCertificateFileName("test.pdf")
                    .contactPersonName("담당").contactPersonPhone("010-0000-0001").status(CompanyStatus.APPROVED).build();
            em.persist(company);
            var category=EventCategory.builder().name("체험").build();
            em.persist(category);
            var now=LocalDateTime.now();
            var event=Event.builder().company(company).category(category).address("부산광역시 해운대구 APEC로 55")
                    .title("참가 흐름 테스트").summary("회차 선택 검증").description("테스트")
                    .status(EventStatus.PUBLISHED).applyStartAt(now.minusDays(1)).applyEndAt(now.plusDays(2)).publishedAt(now).build();
            em.persist(event);
            var session=EventSession.builder().event(event).name("저녁 회차").capacity(capacity).occupiedCount(0)
                    .startsAt(now.plusDays(7)).endsAt(now.plusDays(7).plusHours(2)).nextReceptionNumber(1)
                    .holdDurationSeconds(300).pricingType(fee==0?"FREE":"PAID").fee(fee).reservationDepositPoints(0)
                    .minimumParticipants(1).minimumCheckAt(now.plusDays(6)).status(EventSessionStatus.SCHEDULED).build();
            em.persist(session);
            var user=createUser(points);
            return new Fixture(user.getId(),event.getId(),session.getId());
        });
    }

    private User createUser(int points) {
        int n=sequence.incrementAndGet();
        var user=User.builder().displayName("흐름회원"+n).emailNormalized("flow-member"+n+"@example.test")
                .passwordHash("test").role(Role.MEMBER).point(points).build();
        em.persist(user);
        return user;
    }
    private int balance(Long id) { return jdbc.queryForObject("select point from users where id=?",Integer.class,id); }
    private int occupied(Long id) { return jdbc.queryForObject("select occupied_count from event_sessions where id=?",Integer.class,id); }
    private int rows(String table,Long sessionId) { return jdbc.queryForObject("select count(*) from "+table+" where event_session_id=?",Integer.class,sessionId); }
    private LoginIdentity identity(Long id) { return LoginIdentity.builder().id(id).loginType(LoginType.USER).build(); }
    private MockHttpSession login(Long id) {
        var session=new MockHttpSession();
        session.setAttribute(LoginSession.IDENTITY,identity(id));
        session.setAttribute(LoginSession.USER,SessionUserDto.builder().id(id).displayName("회원").role(Role.MEMBER).build());
        return session;
    }
    private record Fixture(Long userId,Long eventId,Long sessionId) {}
}
