package com.busan.eventplatform.event;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventResponse;
import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EventRecruitmentRenderingTest {
    @Test
    void cardWithoutSessionsRendersFallbackInsteadOfMissingEventDates() throws Exception {
        var event = Event.builder().id(1L).title("테스트 행사").summary("소개").build();
        String template = Files.readString(Path.of("src/main/resources/templates/layout/event-card.mustache"));
        String html = Mustache.compiler().compile(template).execute(new EventResponse.EventDto(event));
        assertThat(html).contains("등록된 회차가 없습니다.");
    }

    @Test
    void cardShowsDifferentRecruitmentPeriodsForEachSession() throws Exception {
        var event = Event.builder().id(1L).title("테스트 행사").summary("소개").build();
        var dto = new EventResponse.EventDto(event);
        dto.setSessionPeriods(java.util.List.of(period("오전", "2030-12-01T09:00", "2030-12-02T18:00"),
                period("오후", "2030-12-03T09:00", "2030-12-04T18:00")));
        String html = Mustache.compiler().compile(Files.readString(
                Path.of("src/main/resources/templates/layout/event-card.mustache"))).execute(dto);
        assertThat(html).contains("오전", "오후", "2030.12.01 09:00 ~ 2030.12.02 18:00",
                "2030.12.03 09:00 ~ 2030.12.04 18:00").doesNotContain("등록된 회차가 없습니다.");
    }

    private EventResponse.SessionPeriodDto period(String name, String start, String end) {
        return new EventResponse.SessionPeriodDto(EventSession.builder().name(name)
                .applyStartAt(java.time.LocalDateTime.parse(start))
                .applyEndAt(java.time.LocalDateTime.parse(end)).build());
    }
}
