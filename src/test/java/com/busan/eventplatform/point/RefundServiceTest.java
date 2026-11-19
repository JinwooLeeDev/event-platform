
package com.busan.eventplatform.point;

import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.DepositRepository;
import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.api.purchase.PurchaseRepository;
import com.busan.eventplatform.api.purchase.PurchaseStatus;
import com.busan.eventplatform.api.refund.Refund;
import com.busan.eventplatform.api.refund.RefundRepository;
import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.api.refund.RefundStatus;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.event.eventSession.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final long USER_ID = 1L;
    private static final long COMPANY_ID = 2L;
    private static final long SESSION_ID = 10L;
    private static final long PURCHASE_ID = 100L;
    private static final long DEPOSIT_ID = 200L;

    @Mock private PurchaseRepository purchaseRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private EventSessionRepository eventSessionRepository;
    @Mock private DepositRepository depositRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefundService refundService;

    @Test
    void 본인_취소가_시작_3일_전이면_6000P_중_4000P를_돌려준다() {
        User user = createUser(USER_ID);
        EventSession session = EventSession.builder()
                .id(SESSION_ID)
                .status(EventSessionStatus.SCHEDULED)
                .fee(6_000)
                .startsAt(LocalDate.now().plusDays(3).atTime(18, 0))
                .build();
        Purchase purchase = createPurchase(user, session, 6_000);
        Registration registration = Registration.create(
                user, session, purchase, RegistrationStatus.CONFIRMED, 1);
        registration.cancel();

        when(purchaseRepository.existsEventByUser(USER_ID, SESSION_ID, PurchaseStatus.PURCHASED))
                .thenReturn(true);
        when(purchaseRepository.findById(PURCHASE_ID)).thenReturn(Optional.of(purchase));

        refundService.refundUser(USER_ID, registration, RegistrationStatus.CONFIRMED);

        assertEquals(14_000, user.getPoint());
        assertEquals(PurchaseStatus.REFUNDED, purchase.getStatus());
        assertSavedUserRefund(user, purchase, 4_000);
    }

    @Test
    void 본인_취소_시_구매_이력이_없으면_잔액과_환불_기록을_변경하지_않는다() {
        User user = createUser(USER_ID);
        EventSession session = EventSession.builder().id(SESSION_ID).build();
        Purchase purchase = createPurchase(user, session, 6_000);
        Registration registration = Registration.create(
                user, session, purchase, RegistrationStatus.CONFIRMED, 1);
        registration.cancel();

        when(purchaseRepository.existsEventByUser(USER_ID, SESSION_ID, PurchaseStatus.PURCHASED))
                .thenReturn(false);

        assertThrows(Exception400.class, () ->
                refundService.refundUser(USER_ID, registration, RegistrationStatus.CONFIRMED));

        assertEquals(10_000, user.getPoint());
        assertEquals(PurchaseStatus.PURCHASED, purchase.getStatus());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void 기업이_회차_3일_전에_취소를_요청하면_보증금의_삼분의이를_환불한다() {
        long companyId = 1L;
        long sessionId = 10L;
        long depositId = 200L;

        Company company = Company.builder()
                .name("테스트 기업")
                .build();
        ReflectionTestUtils.setField(company, "id", companyId);
        company.chargePoint(10_000);

        LocalDate startsOn = LocalDate.of(2026, 11, 25);
        LocalDate requestedOn = startsOn.minusDays(3);

        EventSession session = EventSession.builder()
                .id(sessionId)
                .status(EventSessionStatus.CANCELLED)
                .startsAt(startsOn.atTime(18, 0))
                .build();

        Deposit deposit = Deposit.builder()
                .company(company)
                .eventSession(session)
                .build();
        ReflectionTestUtils.setField(deposit, "id", depositId);
        deposit.setDepositPaid();

        String reason = "기업 사정으로 취소";
        Refund pendingRefund = Refund.companyBuilder()
                .company(company)
                .deposit(deposit)
                .reason(reason)
                .build();

        when(eventSessionRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.of(session));
        when(depositRepository.findByCompanyIdAndSessionId(companyId, sessionId))
                .thenReturn(Optional.of(deposit));
        when(refundRepository.findByDepositId(depositId, RefundStatus.PENDING))
                .thenReturn(Optional.of(pendingRefund));

        refundService.refundCompany(company, sessionId, requestedOn, reason);

        assertEquals(110_000, company.getPoint());
        assertEquals(100_000, pendingRefund.getAmount());
        assertEquals(RefundStatus.REFUND, pendingRefund.getStatus());
        assertEquals(DepositStatus.REFUNDED, deposit.getStatus());
    }

@Test
void 취소되지_않은_회차는_참가자_일괄_환불을_거절한다() {
    EventSession session = EventSession.builder()
            .id(SESSION_ID)
            .status(EventSessionStatus.SCHEDULED)
            .build();

    assertThrows(Exception400.class, () -> refundService.pointBackUser(session));

    verifyNoInteractions(registrationRepository, refundRepository);
}

@Test
void 정상_종료_시_보증금을_한번만_반환하고_별도_환불_기록을_만들지_않는다() {
    Company company = createCompany();
    EventSession session = EventSession.builder()
            .id(SESSION_ID)
            .event(Event.builder().company(company).build())
            .status(EventSessionStatus.COMPLETED)
            .build();
    Deposit deposit = preparePaidDeposit(company, session);

    refundService.refundEventCompleted(COMPANY_ID, SESSION_ID);
    assertThrows(Exception400.class, () ->
            refundService.refundEventCompleted(COMPANY_ID, SESSION_ID));

    assertEquals(160_000, company.getPoint());
    assertEquals(DepositStatus.RETURN, deposit.getStatus());
    verifyNoInteractions(refundRepository);
}

@Test
void 주최_기업이_신청을_반려하면_연결된_구매를_전액_환불한다() {
    User user = createUser(USER_ID);
    EventSession session = EventSession.builder().id(SESSION_ID).fee(6_000).build();
    Purchase purchase = createPurchase(user, session, 6_000);
    Registration registration = Registration.create(
            user, session, purchase, RegistrationStatus.CONFIRMED, 1);
    registration.reject();

    when(userRepository.LockFindById(USER_ID)).thenReturn(Optional.of(user));
    when(purchaseRepository.existsEventByUser(USER_ID, SESSION_ID, PurchaseStatus.PURCHASED))
            .thenReturn(true);

    refundService.refundByCompany(registration);

    assertEquals(16_000, user.getPoint());
    assertEquals(PurchaseStatus.REFUNDED, purchase.getStatus());
    assertSavedUserRefund(user, purchase, 6_000);
}

@Test
void 반려된_신청에_연결된_구매가_없으면_포인트를_지급하지_않는다() {
    User user = createUser(USER_ID);
    EventSession session = EventSession.builder().id(SESSION_ID).build();
    Registration registration = Registration.create(
            user, session, null, RegistrationStatus.CONFIRMED, 1);
    registration.reject();

    when(userRepository.LockFindById(USER_ID)).thenReturn(Optional.of(user));

    assertThrows(Exception404.class, () -> refundService.refundByCompany(registration));

    assertEquals(10_000, user.getPoint());
    verifyNoInteractions(purchaseRepository, refundRepository);
}

private User createUser(long id) {
    User user = User.builder().point(10_000).build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
}

private Company createCompany() {
    Company company = Company.builder().name("테스트 기업").build();
    ReflectionTestUtils.setField(company, "id", COMPANY_ID);
    company.chargePoint(10_000);
    return company;
}

private Purchase createPurchase(User user, EventSession session, int price) {
    Purchase purchase = Purchase.builder().user(user).eventSession(session).price(price).build();
    ReflectionTestUtils.setField(purchase, "id", PURCHASE_ID);
    return purchase;
}

private Deposit preparePaidDeposit(Company company, EventSession session) {
    Deposit deposit = Deposit.builder().company(company).eventSession(session).build();
    ReflectionTestUtils.setField(deposit, "id", DEPOSIT_ID);
    deposit.setDepositPaid();

    when(eventSessionRepository.findByCompanyIdAndSessionId(COMPANY_ID, SESSION_ID))
            .thenReturn(Optional.of(session));
    when(depositRepository.findByCompanyIdAndSessionId(COMPANY_ID, SESSION_ID))
            .thenReturn(Optional.of(deposit));
    return deposit;
}

private void assertSavedUserRefund(User user, Purchase purchase, int expectedAmount) {
    ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
    verify(refundRepository).save(captor.capture());
    assertRefund(captor.getValue(), user, purchase, expectedAmount);
}

private void assertRefund(Refund refund, User user, Purchase purchase, int expectedAmount) {
    assertAll(
            () -> assertSame(user, refund.getUser()),
            () -> assertSame(purchase, refund.getPurchase()),
            () -> assertEquals(expectedAmount, refund.getAmount()),
            () -> assertEquals(RefundStatus.REFUND, refund.getStatus())
    );
}
}
