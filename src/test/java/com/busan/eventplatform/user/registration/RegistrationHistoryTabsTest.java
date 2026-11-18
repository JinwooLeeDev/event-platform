//package com.busan.eventplatform.user.registration;
//
//import com.busan.eventplatform.api.refund.RefundService;
//import com.busan.eventplatform.event.EventSession;
//import com.busan.eventplatform.event.event_category.EventCategory;
//import com.busan.eventplatform.event.event_session.EventSessionRepository;
//import com.busan.eventplatform.event.event_session.EventSessionStatus;
//import com.busan.eventplatform.event.events.Event;
//import com.busan.eventplatform.event.events.EventStatus;
//import com.busan.eventplatform.login.LoginIdentity;
//import com.busan.eventplatform.login.LoginType;
//import com.busan.eventplatform.user.User;
//import com.busan.eventplatform.user.UserRepository;
//import org.junit.jupiter.api.Test;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//class RegistrationHistoryTabsTest {
//    private final RegistrationRepository repository = mock(RegistrationRepository.class);
////    private final RegistrationService service = new RegistrationService(repository,
////            mock(RegistrationHistoryRepository.class), mock(EventSessionRepository.class),
////            mock(UserRepository.class), mock(RefundService.class));
//    private final LoginIdentity identity = LoginIdentity.builder().id(1L).loginType(LoginType.USER).build();
//    private final LocalDateTime now = LocalDateTime.now();
//
//    private Registration record(String title, RegistrationStatus status, int startHours, int endHours,
//                                EventSessionStatus sessionStatus) {
//        var event = Event.builder().id(1L).title(title).address("부산")
//                .category(EventCategory.builder().name("전시").build()).status(EventStatus.PUBLISHED).build();
//        var session = EventSession.builder().id(1L).name("오전 회차").event(event)
//                .startsAt(now.plusHours(startHours)).endsAt(now.plusHours(endHours)).status(sessionStatus).build();
//        var registration = Registration.create(User.builder().build(), session, null,
//                status == RegistrationStatus.CANCELLED || status == RegistrationStatus.REJECTED
//                        ? RegistrationStatus.CONFIRMED : status, 1);
//        if (status == RegistrationStatus.CANCELLED) registration.cancel();
//        if (status == RegistrationStatus.REJECTED) registration.reject();
//        return registration;
//    }
//    private Registration future(String title, RegistrationStatus status) {
//        return record(title, status, 72, 96, EventSessionStatus.SCHEDULED);
//    }
//    private void records(Registration... registrations) {
//        when(repository.findAllByUserIdForHistory(1L)).thenReturn(List.of(registrations));
//    }
//    @Test void defaultAndLegacyTabsShowOnlyCurrentConfirmedRegistrations() {
//        records(future("확정", RegistrationStatus.CONFIRMED), future("대기", RegistrationStatus.WAITING),
//                record("종료", RegistrationStatus.CONFIRMED, -48, -24, EventSessionStatus.COMPLETED));
//        for (String tab : Arrays.asList(null, "", "all", "upcoming", "invalid", "confirmed")) {
//            var history = service.getMyHistory(identity, tab, "");
//            assertThat(history.getCurrentTab()).isEqualTo("confirmed");
//            assertThat(history.getItems()).extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("확정");
//        }
//    }
//    @Test void inProgressSessionStaysConfirmedUntilEndTime() {
//        records(record("진행 중", RegistrationStatus.CONFIRMED, -1, 1, EventSessionStatus.IN_PROGRESS),
//                record("종료시각 경과", RegistrationStatus.CONFIRMED, -2, -1, EventSessionStatus.SCHEDULED));
//        assertThat(service.getMyHistory(identity, "confirmed", "").getItems())
//                .extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("진행 중");
//        assertThat(service.getMyHistory(identity, "past", "").getItems())
//                .extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("종료시각 경과");
//    }
//    @Test void cancelledAndRejectedRegistrationsAndCancelledSessionsAreExcludedFromAllCounts() {
//        records(future("확정", RegistrationStatus.CONFIRMED), future("취소", RegistrationStatus.CANCELLED),
//                future("거절", RegistrationStatus.REJECTED),
//                record("취소된 회차", RegistrationStatus.CONFIRMED, 24, 48, EventSessionStatus.CANCELLED));
//        var history = service.getMyHistory(identity, "confirmed", "");
//        assertThat(history.getTotalCount()).isEqualTo(1);
//        assertThat(history.getConfirmedCount()).isEqualTo(1);
//        assertThat(history.getWaitingCount()).isZero();
//        assertThat(history.getPastCount()).isZero();
//        assertThat(history.getItems()).extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("확정");
//    }
//    @Test void endedWaitingAndExplicitlyCompletedSessionsMoveToPastWithoutCancelAction() {
//        records(future("현재 대기", RegistrationStatus.WAITING),
//                record("종료된 대기", RegistrationStatus.WAITING, -48, -24, EventSessionStatus.COMPLETED),
//                record("조기 종료", RegistrationStatus.CONFIRMED, 24, 48, EventSessionStatus.COMPLETED));
//        assertThat(service.getMyHistory(identity, "waiting", "").getItems())
//                .extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("현재 대기");
//        var past = service.getMyHistory(identity, "past", "");
//        assertThat(past.getItems()).extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("종료된 대기", "조기 종료");
//        assertThat(past.getItems()).allMatch(item -> !item.isCancelable());
//    }
//    @Test void keywordFiltersSelectedTabWhileCountsStayUnfiltered() {
//        records(future("전시 A", RegistrationStatus.CONFIRMED), future("체험 B", RegistrationStatus.CONFIRMED),
//                future("대기 A", RegistrationStatus.WAITING));
//        var history = service.getMyHistory(identity, "confirmed", " A ");
//        assertThat(history.getItems()).extracting(RegistrationRes.MyHistoryItemDto::getTitle).containsExactly("전시 A");
//        assertThat(history.getConfirmedCount()).isEqualTo(2);
//        assertThat(history.getWaitingCount()).isEqualTo(1);
//        assertThat(history.getKeyword()).isEqualTo("A");
//        assertThat(service.getMyHistory(identity, "past", "").isEmpty()).isTrue();
//    }
//
//    @Test void cancelledParentEventIsHiddenWithoutChangingStoredRegistration() {
//        var registration = future("취소된 행사", RegistrationStatus.CONFIRMED);
//        registration.getEventSession().getEvent().setStatus(EventStatus.CANCELLED);
//        records(registration);
//        var history = service.getMyHistory(identity, "confirmed", "");
//        assertThat(history.getTotalCount()).isZero();
//        assertThat(history.isEmpty()).isTrue();
//        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
//        verify(repository).findAllByUserIdForHistory(1L);
//        verifyNoMoreInteractions(repository);
//    }
//}
