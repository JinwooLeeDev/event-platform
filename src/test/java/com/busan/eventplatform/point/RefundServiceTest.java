//package com.busan.eventplatform.point;
//
//import com.busan.eventplatform.api.purchase.Deposit;
//import com.busan.eventplatform.api.purchase.DepositRepository;
//import com.busan.eventplatform.api.purchase.DepositStatus;
//import com.busan.eventplatform.api.purchase.Purchase;
//import com.busan.eventplatform.api.purchase.PurchaseRepository;
//import com.busan.eventplatform.api.purchase.PurchaseStatus;
//import com.busan.eventplatform.api.refund.Refund;
//import com.busan.eventplatform.api.refund.RefundPolicy;
//import com.busan.eventplatform.api.refund.RefundRepository;
//import com.busan.eventplatform.api.refund.RefundService;
//import com.busan.eventplatform.api.refund.RefundStatus;
//import com.busan.eventplatform.company.Company;
//import com.busan.eventplatform.event.EventSession;
//import com.busan.eventplatform.event.event_session.EventSessionRepository;
//import com.busan.eventplatform.event.event_session.EventSessionStatus;
//import com.busan.eventplatform.event.events.Event;
//import com.busan.eventplatform.global.error.exception.Exception400;
//import com.busan.eventplatform.user.User;
//import com.busan.eventplatform.user.registration.Registration;
//import com.busan.eventplatform.user.registration.RegistrationRepository;
//import com.busan.eventplatform.user.registration.RegistrationStatus;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RefundServiceTest {
//
//    @Mock private PurchaseRepository purchaseRepository;
//    @Mock private RefundRepository refundRepository;
//    @Mock private EventSessionRepository eventSessionRepository;
//    @Mock private DepositRepository depositRepository;
//    @Mock private RegistrationRepository registrationRepository;
//    @Mock private User user;
//
//    @InjectMocks
//    private RefundService refundService;
//
//    @Test
//    void 본인_취소가_회차_3일_전이면_결제액의_삼분의이를_환불한다() {
//        EventSession session = EventSession.builder()
//                .id(10L)
//                .status(EventSessionStatus.SCHEDULED)
//                .fee(6_000)
//                .startsAt(LocalDate.now().plusDays(3).atTime(18, 0))
//                .build();
//        Purchase purchase = createPurchase(session, 6_000);
//        Registration registration = Registration.create(
//                user, session, purchase, RegistrationStatus.CONFIRMED, 1
//        );
//        registration.cancel();
//
//        when(purchaseRepository.existsEventByUser(1L, 10L, PurchaseStatus.PURCHASED))
//                .thenReturn(true);
//        when(purchaseRepository.findById(100L)).thenReturn(Optional.of(purchase));
//
//        refundService.refundUser(1L, registration, RegistrationStatus.CONFIRMED);
//
//        verify(user).addPoint(4_000);
//        assertEquals(PurchaseStatus.REFUNDED, purchase.getStatus());
//        assertSavedUserRefund(purchase, 4_000);
//    }
//
//    @Test
//    void 구매_이력이_없으면_포인트를_적립하지_않는다() {
//        EventSession session = EventSession.builder().id(10L).build();
//        Purchase purchase = createPurchase(session, 6_000);
//        Registration registration = Registration.create(
//                user, session, purchase, RegistrationStatus.CONFIRMED, 1
//        );
//        registration.cancel();
//
//        when(purchaseRepository.existsEventByUser(1L, 10L, PurchaseStatus.PURCHASED))
//                .thenReturn(false);
//
//        assertThrows(Exception400.class, () ->
//                refundService.refundUser(1L, registration, RegistrationStatus.CONFIRMED));
//
//        assertEquals(PurchaseStatus.PURCHASED, purchase.getStatus());
//        verify(user, never()).addPoint(anyInt());
//        verifyNoInteractions(refundRepository);
//    }
//
//    @Test
//    void 회차가_취소되면_신청을_반려하고_전액을_한번만_환불한다() {
//        EventSession session = EventSession.builder()
//                .id(10L)
//                .status(EventSessionStatus.CANCELLED)
//                .build();
//        Purchase purchase = createPurchase(session, 6_000);
//        Registration registration = Registration.create(
//                user, session, purchase, RegistrationStatus.WAITING, 1
//        );
//
//        when(registrationRepository.findAllBySessionIdWithUser(10L))
//                .thenReturn(List.of(registration));
//
//        refundService.pointBackUser(session);
//        refundService.pointBackUser(session);
//
//        assertEquals(RegistrationStatus.REJECTED, registration.getStatus());
//        assertEquals(PurchaseStatus.REFUNDED, purchase.getStatus());
//        verify(user, times(1)).addPoint(6_000);
//        assertSavedUserRefund(purchase, 6_000);
//    }
//
//    @Test
//    void 기업_취소_환급은_요청일의_정책을_적용해_해당_보증금을_처리한다() {
//        Company company = mock(Company.class);
//        Deposit deposit = mock(Deposit.class);
//        Refund pendingRefund = mock(Refund.class);
//
//        LocalDate startsOn = LocalDate.of(2026, 11, 25);
//        LocalDate requestedOn = startsOn.minusDays(3);
//        EventSession session = EventSession.builder()
//                .id(10L)
//                .status(EventSessionStatus.CANCELLED)
//                .startsAt(startsOn.atTime(18, 0))
//                .build();
//
//        when(company.getId()).thenReturn(1L);
//        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
//                .thenReturn(Optional.of(session));
//        when(depositRepository.findByCompanyIdAndSessionId(1L, 10L))
//                .thenReturn(Optional.of(deposit));
//        when(deposit.getId()).thenReturn(200L);
//        when(deposit.getStatus()).thenReturn(DepositStatus.PAID);
//        when(refundRepository.findByDepositId(200L, RefundStatus.PENDING))
//                .thenReturn(Optional.of(pendingRefund));
//
//        int expectedAmount = RefundPolicy.forCompanyCancel(
//                Deposit.SESSION_DEPOSIT, requestedOn, startsOn
//        );
//
//        refundService.refundCompany(company, 10L, requestedOn);
//
//        verify(company).chargePoint(expectedAmount);
//        verify(pendingRefund).setCompanyRefund(expectedAmount);
//        verify(deposit).setDepositRefunded();
//    }
//
//    @Test
//    void 정상_종료된_회차는_해당_회차의_보증금을_전액_반환한다() {
//        Company company = mock(Company.class);
//        Deposit deposit = mock(Deposit.class);
//        Event event = Event.builder().company(company).build();
//        EventSession session = EventSession.builder()
//                .id(10L)
//                .event(event)
//                .status(EventSessionStatus.COMPLETED)
//                .build();
//
//        when(eventSessionRepository.findByCompanyIdAndSessionId(1L, 10L))
//                .thenReturn(Optional.of(session));
//        when(depositRepository.findByCompanyIdAndSessionId(1L, 10L))
//                .thenReturn(Optional.of(deposit));
//        when(deposit.getStatus()).thenReturn(DepositStatus.PAID);
//        when(deposit.getAmount()).thenReturn(150_000);
//
//        refundService.refundEventCompleted(1L, 10L);
//
//        verify(company).chargePoint(150_000);
//        verify(deposit).setDepositReturned();
//    }
//
//    private Purchase createPurchase(EventSession session, int price) {
//        Purchase purchase = Purchase.builder()
//                .user(user)
//                .eventSession(session)
//                .price(price)
//                .build();
//        ReflectionTestUtils.setField(purchase, "id", 100L);
//        return purchase;
//    }
//
//    private void assertSavedUserRefund(Purchase purchase, int amount) {
//        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
//        verify(refundRepository, times(1)).save(captor.capture());
//
//        Refund saved = captor.getValue();
//        assertSame(user, saved.getUser());
//        assertSame(purchase, saved.getPurchase());
//        assertEquals(amount, saved.getAmount());
//        assertEquals(RefundStatus.REFUND, saved.getStatus());
//    }
//}