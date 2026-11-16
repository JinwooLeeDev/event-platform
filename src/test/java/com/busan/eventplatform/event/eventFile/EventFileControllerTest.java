
package com.busan.eventplatform.event.eventFile;

import com.busan.eventplatform.event.event_file.EventFile;
import com.busan.eventplatform.event.event_file.EventFileController;
import com.busan.eventplatform.event.event_file.EventFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFileControllerTest {

    @Mock
    private EventFileService eventFileService;

    @Mock
    private Resource resource;

    @Mock
    private EventFile eventFile;

    @InjectMocks
    private EventFileController eventFileController;

    private final Long eventId = 100L;

    @Test
    @DisplayName("행사 대표 이미지 조회 API가 파일을 반환한다")
    void getFile_success() {
        when(eventFileService.getFile(eventId))
                .thenReturn(eventFile);

        when(eventFile.getStorageKey())
                .thenReturn("events/test-image.jpg");

        when(eventFileService.loadFile("events/test-image.jpg"))
                .thenReturn(resource);

        ResponseEntity<Resource> response =
                eventFileController.getFile(eventId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                MediaType.APPLICATION_OCTET_STREAM,
                response.getHeaders().getContentType()
        );
        assertSame(resource, response.getBody());

        verify(eventFileService).getFile(eventId);
        verify(eventFileService).loadFile("events/test-image.jpg");
    }
}
