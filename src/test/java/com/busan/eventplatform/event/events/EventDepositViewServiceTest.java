package com.busan.eventplatform.event.events;

import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.DepositRepository;
import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCategory.EventCategoryRepository;
import com.busan.eventplatform.event.eventFile.EventFileService;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.event.eventSession.EventSessionService;
import com.busan.eventplatform.event.eventSession.EventSessionStatus;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDepositViewServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventCategoryRepository eventCategoryRepository;
    @Mock CompanyRepository companyRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock EventSessionService eventSessionService;
    @Mock EventSessionRepository eventSessionRepository;
    @Mock EventFileService eventFileService;
    @Mock DepositRepository depositRepository;

    @InjectMocks EventService eventService;

    private static final Long COMPANY_ID = 1L;
    private static final Long EVENT_ID = 100L;

    @Test
    void 모두_미납이면_각_회차에_결제버튼을_표시한다() {
        Event event = event();
        EventSession first = session(event, 10L);
        EventSession second = session(event, 11L);

        prepare(event, List.of(first, second), List.of(),
                CompanyStatus.APPROVED);

        EventResponse.EventDto result = findEvent();

        assertEquals(2, result.getSessionCount());
        assertEquals(0, result.getPaidSessionCount());
        assertEquals(2, result.getUnpaidSessionCount());

        for (EventResponse.SessionPeriodDto period : result.getSessionPeriods()) {
            assertEquals("미납", period.getDepositStatusName());
            assertEquals("150,000", period.getDepositAmountText());
            assertNull(period.getDepositPaidAt());
            assertTrue(period.isDepositPayable());
        }
    }

    @Test
    void 일부_납부하면_납부회차의_버튼을_숨기고_집계를_표시한다() {
        Event event = event();
        EventSession first = session(event, 10L);
        EventSession second = session(event, 11L);

        Deposit deposit = deposit(first, DepositStatus.PAID);

        prepare(event, List.of(first, second), List.of(deposit),
                CompanyStatus.APPROVED);

        EventResponse.EventDto result = findEvent();

        EventResponse.SessionPeriodDto paid = result.getSessionPeriods().get(0);
        EventResponse.SessionPeriodDto unpaid = result.getSessionPeriods().get(1);

        assertEquals(1, result.getPaidSessionCount());
        assertEquals(1, result.getUnpaidSessionCount());

        assertEquals("결제 완료", paid.getDepositStatusName());
        assertEquals("150,000", paid.getDepositAmountText());
        assertEquals("2026.11.19 10:00", paid.getDepositPaidAt());
        assertFalse(paid.isDepositPayable());

        assertEquals("미납", unpaid.getDepositStatusName());
        assertTrue(unpaid.isDepositPayable());

        // 회차마다 조회하지 않고 목록으로 한 번 조회하는지 확인
        verify(depositRepository).findAllByCompanyIdAndSessionIds(
                COMPANY_ID, List.of(10L, 11L)
        );
    }

    @Test
    void 모두_납부했으면_결제버튼이_없고_미납은_0건이다() {
        Event event = event();
        EventSession first = session(event, 10L);
        EventSession second = session(event, 11L);

        prepare(
                event,
                List.of(first, second),
                List.of(
                        deposit(first, DepositStatus.PAID),
                        deposit(second, DepositStatus.PAID)
                ),
                CompanyStatus.APPROVED
        );

        EventResponse.EventDto result = findEvent();

        assertEquals(2, result.getPaidSessionCount());
        assertEquals(0, result.getUnpaidSessionCount());
        assertTrue(result.getSessionPeriods().stream()
                .noneMatch(EventResponse.SessionPeriodDto::isDepositPayable));

        // 조회 Service는 행사 공개 상태를 변경하지 않음
        assertEquals(EventStatus.PAID_READY, event.getStatus());
    }

    @Test
    void 환불과_반환은_미납으로_집계하지_않고_재결제버튼도_숨긴다() {
        Event event = event();
        EventSession first = session(event, 10L);
        EventSession second = session(event, 11L);

        prepare(
                event,
                List.of(first, second),
                List.of(
                        deposit(first, DepositStatus.REFUNDED),
                        deposit(second, DepositStatus.RETURN)
                ),
                CompanyStatus.APPROVED
        );

        EventResponse.EventDto result = findEvent();

        assertEquals(0, result.getPaidSessionCount());
        assertEquals(0, result.getUnpaidSessionCount());

        assertEquals(
                DepositStatus.REFUNDED.getDisplayName(),
                result.getSessionPeriods().get(0).getDepositStatusName()
        );
        assertEquals(
                DepositStatus.RETURN.getDisplayName(),
                result.getSessionPeriods().get(1).getDepositStatusName()
        );

        assertTrue(result.getSessionPeriods().stream()
                .noneMatch(EventResponse.SessionPeriodDto::isDepositPayable));
    }

    @Test
    void 미승인_기업은_미납이어도_결제버튼을_표시하지_않는다() {
        Event event = event();
        EventSession session = session(event, 10L);

        prepare(event, List.of(session), List.of(), CompanyStatus.PENDING);

        EventResponse.EventDto result = findEvent();

        assertEquals(1, result.getUnpaidSessionCount());
        assertFalse(result.getSessionPeriods().get(0).isDepositPayable());
    }

    @Test
    void 취소된_회차는_미납이어도_결제버튼을_표시하지_않는다() {
        Event event = event();
        EventSession session = session(event, 10L);
        session.setStatus(EventSessionStatus.CANCELLED);

        prepare(event, List.of(session), List.of(), CompanyStatus.APPROVED);

        assertFalse(findEvent().getSessionPeriods().get(0).isDepositPayable());
    }

    @Test
    void 행사가_없으면_빈목록을_반환하고_보증금을_조회하지_않는다() {
        when(eventRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of());

        EventResponse.ListDto result =
                eventService.findAllByCompanyId(COMPANY_ID);

        assertTrue(result.getEventList().isEmpty());
        verifyNoInteractions(depositRepository, eventSessionRepository);
    }

    // 화면용 조회에 필요한 Repository 결과 설정
    private void prepare(
            Event event,
            List<EventSession> sessions,
            List<Deposit> deposits,
            CompanyStatus companyStatus
    ) {
        Company company = Company.builder()
                .status(companyStatus)
                .build();

        when(eventRepository.findAllByCompanyId(COMPANY_ID))
                .thenReturn(List.of(event));

        when(eventSessionRepository.findAllByEventIds(List.of(EVENT_ID)))
                .thenReturn(sessions);

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        List<Long> sessionIds = sessions.stream()
                .map(EventSession::getId)
                .toList();

        when(depositRepository.findAllByCompanyIdAndSessionIds(
                COMPANY_ID, sessionIds
        )).thenReturn(deposits);
    }

    private EventResponse.EventDto findEvent() {
        return eventService.findAllByCompanyId(COMPANY_ID)
                .getEventList()
                .get(0);
    }

    private Event event() {
        return Event.builder()
                .id(EVENT_ID)
                .title("보증금 테스트 행사")
                .status(EventStatus.PAID_READY)
                .build();
    }

    private EventSession session(Event event, Long id) {
        return EventSession.builder()
                .id(id)
                .event(event)
                .name("회차 " + id)
                .applyStartAt(LocalDateTime.of(2026, 11, 20, 9, 0))
                .applyEndAt(LocalDateTime.of(2026, 11, 25, 18, 0))
                .status(EventSessionStatus.SCHEDULED)
                .build();
    }

    private Deposit deposit(EventSession session, DepositStatus status) {
        Deposit deposit = new Deposit(null, session);

        // DB 없이 상태와 납부 시각을 준비하는 테스트용 설정
        ReflectionTestUtils.setField(deposit, "status", status);
        ReflectionTestUtils.setField(
                deposit,
                "paidAt",
                Timestamp.valueOf("2026-11-19 10:00:00")
        );

        return deposit;
    }
}