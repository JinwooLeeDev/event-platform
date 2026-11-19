//package com.busan.eventplatform.user.registration;
//
//import com.busan.eventplatform.api.purchase.*;
//import com.busan.eventplatform.api.refund.*;
//import com.busan.eventplatform.company.Company;
//import com.busan.eventplatform.event.EventSession;
//import com.busan.eventplatform.event.event_session.EventSessionRepository;
//import com.busan.eventplatform.event.events.Event;
//import com.busan.eventplatform.login.*;
//import com.busan.eventplatform.user.*;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.test.util.ReflectionTestUtils;
//import java.util.Optional;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class RegistrationRejectRefundTest {
//    private static class Fixture {
//        final RegistrationRepository registrations = mock(RegistrationRepository.class);
//        final RegistrationHistoryRepository history = mock(RegistrationHistoryRepository.class);
//        final EventSessionRepository sessions = mock(EventSessionRepository.class);
//        final UserRepository users = mock(UserRepository.class);
//        final PurchaseRepository purchases = mock(PurchaseRepository.class);
//        final RefundRepository refundRecords = mock(RefundRepository.class);
//        final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
//        final RefundService refunds = new RefundService(purchases, refundRecords, sessions, mock(DepositRepository.class), registrations, users);
//        final RegistrationService service = new RegistrationService(registrations, history, sessions, users, refunds, events);
//        final LoginIdentity company = LoginIdentity.builder().id(9L).loginType(LoginType.COMPANY).build();
//        final User user = User.builder().point(1000).build();
//        final EventSession session;
//        final Purchase purchase;
//        final Registration registration;
//        Fixture(RegistrationStatus status, int fee, boolean withPurchase) {
//            var owner = mock(Company.class); when(owner.getId()).thenReturn(9L);
//            session = EventSession.builder().id(10L).event(Event.builder().company(owner).build())
//                    .fee(fee).capacity(1).occupiedCount(status.isSeatOccupying() ? 1 : 0).build();
//            ReflectionTestUtils.setField(user, "id", 1L);
//            purchase = withPurchase ? Purchase.builder().user(user).eventSession(session).price(fee).build() : null;
//            if (purchase != null) ReflectionTestUtils.setField(purchase, "id", 100L);
//            registration = Registration.create(user, session, purchase, status, 1);
//            ReflectionTestUtils.setField(registration, "id", 20L);
//            when(registrations.findSessionIdById(20L)).thenReturn(Optional.of(10L));
//            when(sessions.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
//            when(registrations.findById(20L)).thenReturn(Optional.of(registration));
//            when(users.LockFindById(1L)).thenReturn(Optional.of(user));
//            when(purchases.existsEventByUser(1L, 10L, PurchaseStatus.PURCHASED)).thenReturn(true);
//        }
//    }
//
//    @Test void rejectsActiveApplicationsAndRefundsPaidPointsOnlyOnce() {
//        for (var status : new RegistrationStatus[]{RegistrationStatus.CONFIRMED, RegistrationStatus.PENDING, RegistrationStatus.WAITING}) {
//            var f = new Fixture(status, 6000, true);
//            f.service.reject(20L, f.company);
//            assertEquals(RegistrationStatus.REJECTED, f.registration.getStatus());
//            assertEquals(7000, f.user.getPoint());
//            assertEquals(PurchaseStatus.REFUNDED, f.purchase.getStatus());
//            var saved = ArgumentCaptor.forClass(Refund.class);
//            verify(f.refundRecords).save(saved.capture());
//            assertEquals(6000, saved.getValue().getAmount());
//            assertEquals(RefundStatus.REFUND, saved.getValue().getStatus());
//            assertThrows(RuntimeException.class, () -> f.service.reject(20L, f.company));
//            assertEquals(7000, f.user.getPoint());
//            verify(f.refundRecords, times(1)).save(any());
//        }
//    }
//
//    @Test void supportsZeroPointPurchaseAndLegacyApplicationWithoutPurchase() {
//        for (boolean withPurchase : new boolean[]{true, false}) {
//            var f = new Fixture(RegistrationStatus.WAITING, 0, withPurchase);
//            f.service.reject(20L, f.company);
//            assertEquals(RegistrationStatus.REJECTED, f.registration.getStatus());
//            assertEquals(1000, f.user.getPoint());
//            if (withPurchase) {
//                assertEquals(PurchaseStatus.REFUNDED, f.purchase.getStatus());
//                verify(f.refundRecords).save(any());
//            } else verifyNoInteractions(f.refundRecords);
//        }
//    }
//
//    @Test void rejectingConfirmedParticipantPromotesWaitingParticipantWithoutAnotherCharge() {
//        var f = new Fixture(RegistrationStatus.CONFIRMED, 6000, true);
//        var waitingUser = User.builder().point(1000).build();
//        ReflectionTestUtils.setField(waitingUser, "id", 2L);
//        var waitingPurchase = Purchase.builder().user(waitingUser).eventSession(f.session).price(6000).build();
//        var waiting = Registration.create(waitingUser, f.session, waitingPurchase, RegistrationStatus.WAITING, 2);
//        when(f.registrations.findFirstByEventSessionIdAndStatusOrderByReceptionNumberAsc(10L, RegistrationStatus.WAITING))
//                .thenReturn(Optional.of(waiting));
//        f.service.reject(20L, f.company);
//        assertEquals(RegistrationStatus.CONFIRMED, waiting.getStatus());
//        assertEquals(1, f.session.getOccupiedCount());
//        assertEquals(1000, waitingUser.getPoint());
//        assertEquals(PurchaseStatus.PURCHASED, waitingPurchase.getStatus());
//        assertEquals(7000, f.user.getPoint());
//        verify(f.refundRecords, times(1)).save(any());
//    }
//
//    @Test void unauthorizedOrganizerCannotRejectOrRefund() {
//        var f = new Fixture(RegistrationStatus.CONFIRMED, 6000, true);
//        assertThrows(RuntimeException.class, () -> f.service.reject(20L,
//                LoginIdentity.builder().id(99L).loginType(LoginType.COMPANY).build()));
//        assertEquals(RegistrationStatus.CONFIRMED, f.registration.getStatus());
//        assertEquals(1000, f.user.getPoint());
//        verifyNoInteractions(f.refundRecords, f.history, f.events);
//    }
//
//    @Test void refundFailurePropagatesBeforeSeatReleaseAndEvents() {
//        var f = new Fixture(RegistrationStatus.CONFIRMED, 6000, true);
//        when(f.purchases.existsEventByUser(1L, 10L, PurchaseStatus.PURCHASED)).thenReturn(false);
//        assertThrows(RuntimeException.class, () -> f.service.reject(20L, f.company));
//        assertEquals(1, f.session.getOccupiedCount());
//        assertEquals(1000, f.user.getPoint());
//        verifyNoInteractions(f.refundRecords, f.history, f.events);
//    }
//}
