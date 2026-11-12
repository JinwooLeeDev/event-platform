package com.busan.eventplatform.event.events;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.event_category.EventCategory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventResponseTest {

    // 1. 행사 DTO 변환
    @Test
    void eventDto_test() {
        // given
        Event event = createEvent();

        // when
        EventResponse.EventDto dto = new EventResponse.EventDto(event);

        // then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("부산 문화 축제");
        assertThat(dto.getSummary()).isEqualTo("부산 축제 소개");
        assertThat(dto.getCategoryId()).isEqualTo(1L);
        assertThat(dto.getCategoryName()).isEqualTo("축제");
        assertThat(dto.getFavoriteCount()).isZero();
        assertThat(dto.isFavorite()).isFalse();
    }

    // 2. 좋아요 수와 좋아요 여부 포함
    @Test
    void eventDto_favorite_test() {
        // given
        Event event = createEvent();

        // when
        EventResponse.EventDto dto =
                new EventResponse.EventDto(event, 15L, true);

        // then
        assertThat(dto.getFavoriteCount()).isEqualTo(15L);
        assertThat(dto.isFavorite()).isTrue();
    }

    // 3. 행사 카테고리가 없는 경우
    @Test
    void eventDto_category_null_test() {
        // given
        Event event = Event.builder()
                .id(1L)
                .title("테스트 행사")
                .summary("소개")
                .build();

        // when
        EventResponse.EventDto dto = new EventResponse.EventDto(event);

        // then
        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCategoryName()).isNull();
    }

    // 4. 행사 목록 DTO 변환
    @Test
    void listDto_test() {
        // given
        Event event1 = createEvent();

        Event event2 = Event.builder()
                .id(2L)
                .title("부산 전시회")
                .summary("전시회 소개")
                .build();

        // when
        EventResponse.ListDto dto =
                new EventResponse.ListDto(List.of(event1, event2));

        // then
        assertThat(dto.getEventList()).hasSize(2);
        assertThat(dto.getEventList().get(0).getTitle())
                .isEqualTo("부산 문화 축제");
        assertThat(dto.getEventList().get(1).getTitle())
                .isEqualTo("부산 전시회");
    }

    // 5. 페이지 DTO 변환
    @Test
    void pageDto_test() {
        // given
        List<Event> events = List.of(
                createEvent(),
                Event.builder()
                        .id(2L)
                        .title("부산 전시회")
                        .summary("전시회 소개")
                        .build()
        );

        Page<Event> eventPage = new PageImpl<>(
                events,
                PageRequest.of(0, 10),
                25
        );

        // when
        EventResponse.PageDto dto =
                new EventResponse.PageDto(eventPage, List.of());

        // then
        assertThat(dto.getEventList()).hasSize(2);
        assertThat(dto.getCurrentPage()).isEqualTo(1);
        assertThat(dto.getTotalPages()).isEqualTo(3);
        assertThat(dto.getTotalElements()).isEqualTo(25);
        assertThat(dto.getSize()).isEqualTo(10);
        assertThat(dto.isFirst()).isTrue();
        assertThat(dto.isLast()).isFalse();
    }

    // 6. 행사 상세 DTO 변환
    @Test
    void detailDto_test() {
        // given
        Event event = createEvent();

        // when
        EventResponse.DetailDto dto =
                new EventResponse.DetailDto(
                        event,
                        true,
                        10L,
                        3L,
                        true
                );

        // then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("부산 문화 축제");
        assertThat(dto.getCompanyId()).isEqualTo(10L);
        assertThat(dto.getCategoryId()).isEqualTo(1L);
        assertThat(dto.getCategoryName()).isEqualTo("축제");
        assertThat(dto.isOwner()).isTrue();
        assertThat(dto.getConfirmedCount()).isEqualTo(10L);
        assertThat(dto.getWaitingCount()).isEqualTo(3L);
        assertThat(dto.isChatAvailable()).isTrue();
    }

    // 7. 수정 화면 DTO 변환
    @Test
    void updateFormDto_test() {
        // given
        Event event = createEvent();
        event.setFeedbackMessage("주소를 보완해주세요.");

        // when
        EventResponse.UpdateFormDto dto =
                new EventResponse.UpdateFormDto(event);

        // then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("부산 문화 축제");
        assertThat(dto.getCompanyId()).isEqualTo(10L);
        assertThat(dto.getCategoryId()).isEqualTo(1L);
        assertThat(dto.getCategoryName()).isEqualTo("축제");
        assertThat(dto.getFeedbackMessage())
                .isEqualTo("주소를 보완해주세요.");
    }

    private Event createEvent() {
        Company company = mock(Company.class);
        when(company.getId()).thenReturn(10L);

        EventCategory category = mock(EventCategory.class);
        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("축제");

        return Event.builder()
                .id(1L)
                .company(company)
                .category(category)
                .title("부산 문화 축제")
                .summary("부산 축제 소개")
                .description("다양한 문화 행사")
                .address("부산 해운대구")
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();
    }
}