package com.busan.eventplatform.event.events;

import com.busan.eventplatform.user.registration.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRepositoryTest {

    @Mock
    private EventRepository eventRepository;

    // 1. 공개 행사 목록 조회
    @Test
    void findAllPublished_test() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Event> page = new PageImpl<>(
                Collections.emptyList()
        );

        when(eventRepository.findAllPublished(
                EventStatus.PUBLISHED, pageable
        )).thenReturn(page);

        // when
        Page<Event> result = eventRepository.findAllPublished(
                EventStatus.PUBLISHED, pageable
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(eventRepository).findAllPublished(
                EventStatus.PUBLISHED, pageable
        );
    }

    // 2. 공개 행사 키워드 검색
    @Test
    void findAllKeyWord_test() {
        // given
        String keyword = "축제";
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Event> page = new PageImpl<>(
                Collections.emptyList()
        );

        when(eventRepository.findAllKeyWord(
                EventStatus.PUBLISHED, keyword, pageable
        )).thenReturn(page);

        // when
        Page<Event> result = eventRepository.findAllKeyWord(
                EventStatus.PUBLISHED, keyword, pageable
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        verify(eventRepository).findAllKeyWord(
                EventStatus.PUBLISHED, keyword, pageable
        );
    }

    // 3. 행사 상세 조회
    @Test
    void findByDetail_test() {
        // given
        Long eventId = 1L;

        Event event = Event.builder()
                .id(eventId)
                .title("부산 문화 축제")
                .build();

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.of(event));

        // when
        Optional<Event> result =
                eventRepository.findByDetail(eventId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle())
                .isEqualTo("부산 문화 축제");

        verify(eventRepository).findByDetail(eventId);
    }

    // 4. 존재하지 않는 행사 상세 조회
    @Test
    void findByDetail_notFound_test() {
        // given
        Long eventId = 999L;

        when(eventRepository.findByDetail(eventId))
                .thenReturn(Optional.empty());

        // when
        Optional<Event> result =
                eventRepository.findByDetail(eventId);

        // then
        assertThat(result).isEmpty();
    }

    // 5. 기업별 보완 요청 행사 조회
    @Test
    void findAllByCompanyIdAndStatus_test() {
        // given
        Long companyId = 1L;

        when(eventRepository.findAllByCompanyIdAndStatus(
                companyId, EventStatus.FEEDBACK_REQUIRED
        )).thenReturn(Collections.emptyList());

        // when
        var result = eventRepository.findAllByCompanyIdAndStatus(
                companyId, EventStatus.FEEDBACK_REQUIRED
        );

        // then
        assertThat(result).isEmpty();

        verify(eventRepository).findAllByCompanyIdAndStatus(
                companyId, EventStatus.FEEDBACK_REQUIRED
        );
    }

    // 6. 기업별 전체 행사 조회
    @Test
    void findAllByCompanyId_test() {
        // given
        Long companyId = 1L;

        when(eventRepository.findAllByCompanyId(companyId))
                .thenReturn(Collections.emptyList());

        // when
        var result = eventRepository.findAllByCompanyId(companyId);

        // then
        assertThat(result).isEmpty();

        verify(eventRepository).findAllByCompanyId(companyId);
    }

    // 7. 참가 확정 인원 조회
    @Test
    void countConfirmedByEventIdAndUserId_test() {
        // given
        Long eventId = 1L;
        Long userId = 1L;

        when(eventRepository.countConfirmedByEventIdAndUserId(
                eventId, userId, RegistrationStatus.CONFIRMED
        )).thenReturn(1L);

        // when
        long result =
                eventRepository.countConfirmedByEventIdAndUserId(
                        eventId, userId, RegistrationStatus.CONFIRMED
                );

        // then
        assertThat(result).isEqualTo(1L);

        verify(eventRepository).countConfirmedByEventIdAndUserId(
                eventId, userId, RegistrationStatus.CONFIRMED
        );
    }

    // 8. 참가하지 않은 회원 조회
    @Test
    void countConfirmed_zero_test() {
        // given
        Long eventId = 1L;
        Long userId = 2L;

        when(eventRepository.countConfirmedByEventIdAndUserId(
                eventId, userId, RegistrationStatus.CONFIRMED
        )).thenReturn(0L);

        // when
        long result =
                eventRepository.countConfirmedByEventIdAndUserId(
                        eventId, userId, RegistrationStatus.CONFIRMED
                );

        // then
        assertThat(result).isZero();
    }
}