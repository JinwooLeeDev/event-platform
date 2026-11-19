package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventCancel.EventCancelRepository;
import com.busan.eventplatform.event.eventCategory.EventCategory;
import com.busan.eventplatform.event.eventSession.*;
import com.busan.eventplatform.event.events.*;
import com.busan.eventplatform.login.*;
import com.busan.eventplatform.user.*;
import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationChangeHistoryTest {
    private final RegistrationRepository registrations = mock(RegistrationRepository.class);
    private final RegistrationHistoryRepository changes = mock(RegistrationHistoryRepository.class);
    private final RegistrationService service = new RegistrationService(registrations, changes,
            mock(EventSessionRepository.class), mock(UserRepository.class), mock(RefundService.class),
            mock(EventCancelRepository.class), mock(ApplicationEventPublisher.class));
    private final LoginIdentity identity = LoginIdentity.builder().id(2L).loginType(LoginType.USER).build();

    @Test
    void loadsVisibleApplicationsInOneOwnerScopedQueryAndKeepsReapplicationsSeparate() {
        var old = registration(10L);
        var current = registration(11L);
        var hidden = registration(12L);
        hidden.cancel();
        when(registrations.findAllByUserIdForHistory(2L)).thenReturn(List.of(current, old, hidden));
        var first = change(old, RegistrationStatus.WAITING, "참가 신청", "2026-11-19T00:00:00Z");
        var second = change(old, RegistrationStatus.CONFIRMED, "선행 대기자 승격", "2026-11-19T01:00:00Z");
        var other = change(current, RegistrationStatus.CONFIRMED, "참가 신청", "2026-11-19T02:00:00Z");
        when(changes.findByUserIdAndRegistrationIds(2L, List.of(11L, 10L))).thenReturn(List.of(first, second, other));

        var result = service.getMyHistory(identity, "confirmed", "");

        assertEquals(2, result.getItems().size());
        assertEquals(1, result.getItems().get(0).getChangeHistory().size());
        var history = result.getItems().get(1).getChangeHistory();
        assertEquals(List.of("대기열", "참가 확정"), history.stream().map(RegistrationRes.ChangeHistoryDto::getStatusLabel).toList());
        assertEquals("2026.11.19 09:00", history.get(0).getOccurredAtText());
        assertEquals("선행 대기자 승격", history.get(1).getReason());
        verify(changes, times(1)).findByUserIdAndRegistrationIds(2L, List.of(11L, 10L));
    }

    @Test
    void personalDisplayOmitsInternalCompanyIdAndEscapesStoredText() throws Exception {
        var registration = registration(10L);
        var rejected = change(registration, RegistrationStatus.REJECTED, "주최자 거절 (companyId=99)", "2026-11-19T00:00:00Z");
        var unsafe = change(registration, RegistrationStatus.CANCELLED, "<script>사유</script>", "2026-11-19T01:00:00Z");
        var item = item().registrationId(10L).title("부산 행사")
                .changeHistory(List.of(new RegistrationRes.ChangeHistoryDto(rejected), new RegistrationRes.ChangeHistoryDto(unsafe))).build();
        var history = RegistrationRes.MyHistoryDto.builder().currentTab("confirmed").keyword("").items(List.of(item)).build();
        String html = render(history);
        assertTrue(html.contains("신청 변경 이력"));
        assertTrue(html.contains("주최자 거절"));
        assertFalse(html.contains("companyId"));
        assertTrue(html.contains("&lt;script&gt;사유&lt;/script&gt;"));
        assertFalse(html.contains("<script>사유</script>"));
        assertFalse(html.contains("processedByUser"));
    }

    @Test
    void showsEmptyHistoryAndSkipsQueryForNoVisibleApplications() throws Exception {
        when(registrations.findAllByUserIdForHistory(2L)).thenReturn(List.of());
        assertTrue(service.getMyHistory(identity, "confirmed", "").isEmpty());
        verifyNoInteractions(changes);
        var item = item().title("과거 신청").build();
        assertTrue(render(RegistrationRes.MyHistoryDto.builder().currentTab("confirmed").keyword("").items(List.of(item)).build())
                .contains("저장된 변경 이력이 없습니다."));
    }

    @Test
    void deniesCompanyBeforeLoadingHistory() {
        var company = LoginIdentity.builder().id(2L).loginType(LoginType.COMPANY).build();
        assertThrows(com.busan.eventplatform.global.error.exception.RegistrationForbiddenException.class,
                () -> service.getMyHistory(company, "confirmed", ""));
        verifyNoInteractions(registrations, changes);
    }

    private Registration registration(Long id) {
        var event = Event.builder().id(7L).title("부산 행사").address("부산").status(EventStatus.PUBLISHED)
                .category(EventCategory.builder().name("체험").build()).build();
        var session = EventSession.builder().id(4L).event(event).name("오후 회차").status(EventSessionStatus.SCHEDULED)
                .startsAt(LocalDateTime.now().plusDays(10)).endsAt(LocalDateTime.now().plusDays(11)).build();
        var registration = Registration.create(User.builder().build(), session, null, RegistrationStatus.CONFIRMED, 1);
        ReflectionTestUtils.setField(registration, "id", id);
        return registration;
    }

    private RegistrationHistory change(Registration registration, RegistrationStatus status, String reason, String time) {
        var history = RegistrationHistory.builder().registration(registration).changedStatus(status).reason(reason).build();
        ReflectionTestUtils.setField(history, "createdAt", Instant.parse(time));
        return history;
    }

    private RegistrationRes.MyHistoryItemDto.MyHistoryItemDtoBuilder item() {
        return RegistrationRes.MyHistoryItemDto.builder().statusClass("status-confirmed").statusLabel("참가 확정")
                .sessionName("오후 회차").scheduleText("일정").place("부산").statusMessage("신청 안내")
                .paidPointText("0P").detailUrl("/event/7");
    }

    private String render(RegistrationRes.MyHistoryDto history) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/user/history-content.mustache"), StandardCharsets.UTF_8)) {
            return Mustache.compiler().compile(reader).execute(Map.of("history", history, "historyTabs", List.of(), "myPoint", 0, "resultCount", 1));
        }
    }
}
