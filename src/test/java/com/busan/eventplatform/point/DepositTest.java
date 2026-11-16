package com.busan.eventplatform.point;

import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.DepositRepository;
import com.busan.eventplatform.api.purchase.DepositService;
import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.error.exception.AlreadyPurchaseException;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositTest {

    private static final long COMPANY_ID = 1L;
    private static final long SESSION_ID = 10L;
    private static final int SESSION_DEPOSIT = 150_000;

    @Mock
    private EventSessionRepository eventSessionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private EventSession eventSession;

    @InjectMocks
    private DepositService depositService;

    @Test
    void 선택한_회차의_보증금만_차감하고_납부내역을_저장한다() {
        Company company = companyWithPoints(500_000);
        Event event = stubApprovedSession(company);

        depositService.payDeposit(COMPANY_ID, SESSION_ID);

        ArgumentCaptor<Deposit> captor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositRepository).save(captor.capture());

        Deposit saved = captor.getValue();
        assertSame(company, saved.getCompany());
        assertSame(eventSession, saved.getEventSession());
        assertEquals(SESSION_DEPOSIT, saved.getAmount());
        assertEquals(DepositStatus.PAID, saved.getStatus());
        assertEquals(350_000, company.getPoint());

        verify(eventSession).setScheduled();
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
    }

    @Test
    void 같은_회차에_다시_결제하면_추가로_차감하지_않는다() {
        Company company = companyWithPoints(500_000);
        stubApprovedSession(company);

        depositService.payDeposit(COMPANY_ID, SESSION_ID);

        assertThrows(AlreadyPurchaseException.class, () ->
                depositService.payDeposit(COMPANY_ID, SESSION_ID));

        assertEquals(350_000, company.getPoint());
        verify(companyRepository).LockFindById(COMPANY_ID);
        verify(eventSession).setScheduled();

        ArgumentCaptor<Deposit> captor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositRepository).save(captor.capture());
    }

    @Test
    void 잔액이_부족하면_회차와_행사_상태를_변경하지_않는다() {
        Company company = companyWithPoints(100_000);
        Event event = stubApprovedSession(company);

        Exception400 exception = assertThrows(Exception400.class, () ->
                depositService.payDeposit(COMPANY_ID, SESSION_ID));

        assertTrue(exception.getMessage().contains("잔액이 부족합니다"));
        assertEquals(100_000, company.getPoint());
        assertEquals(EventStatus.PAID_READY, event.getStatus());
        verify(eventSession, never()).setScheduled();
        verifyNoInteractions(depositRepository);
    }

    private Event stubApprovedSession(Company company) {
        Event event = Event.builder()
                .company(company)
                .status(EventStatus.PAID_READY)
                .build();

        when(eventSessionRepository.findByIdForUpdate(SESSION_ID))
                .thenReturn(Optional.of(eventSession));
        when(eventSession.getEvent()).thenReturn(event);
        when(eventSession.isOwner(COMPANY_ID)).thenReturn(true);
        when(companyRepository.LockFindById(COMPANY_ID))
                .thenReturn(Optional.of(company));

        return event;
    }

    private Company companyWithPoints(int points) {
        Company company = Company.builder().build();
        company.chargePoint(points);
        return company;
    }
}