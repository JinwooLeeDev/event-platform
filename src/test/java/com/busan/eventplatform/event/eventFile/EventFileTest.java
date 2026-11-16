
package com.busan.eventplatform.event.eventFile;

import com.busan.eventplatform.event.event_file.EventFile;
import com.busan.eventplatform.event.events.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventFileTest {

    @Test
    @DisplayName("행사 대표 이미지 정보를 생성한다")
    void create_success() {
        Event event = Event.builder()
                .title("부산 전시 행사")
                .build();

        EventFile eventFile = EventFile.builder()
                .event(event)
                .storageKey("events/test-image.jpg")
                .build();

        assertNotNull(eventFile);
        assertSame(event, eventFile.getEvent());
        assertEquals("events/test-image.jpg", eventFile.getStorageKey());
    }

    @Test
    @DisplayName("대표 이미지 저장 키를 변경한다")
    void changeStorageKey_success() {
        EventFile eventFile = EventFile.builder()
                .storageKey("events/old-image.jpg")
                .build();

        eventFile.changeStorageKey("events/new-image.jpg");

        assertEquals("events/new-image.jpg", eventFile.getStorageKey());
    }
}
