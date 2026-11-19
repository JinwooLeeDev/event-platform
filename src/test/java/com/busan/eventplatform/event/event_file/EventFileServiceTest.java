
package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.global.file.FileStorageResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFileServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventFileRepository eventFileRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private FileStorageResult fileStorageResult;

    @Mock
    private Resource resource;

    @InjectMocks
    private EventFileService eventFileService;

    private Event event;
    private EventFile eventFile;

    private final Long eventId = 100L;
    private final Long fileId = 200L;

    private final String oldStorageKey = "events/old-image.jpg";
    private final String newStorageKey = "events/new-image.jpg";

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(eventId)
                .title("부산 전시 행사")
                .build();

        eventFile = EventFile.builder()
                .id(fileId)
                .event(event)
                .storageKey(oldStorageKey)
                .build();
    }

    // =========================
    // 파일 업로드 및 연결
    // =========================

    @Test
    @DisplayName("행사에 대표 이미지가 없으면 새로 등록한다")
    void attachFile_newFile() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventFileRepository.findByEventId(eventId))
                .thenReturn(Optional.empty());

        when(fileStorage.upload(multipartFile))
                .thenReturn(fileStorageResult);

        when(fileStorageResult.getStorageKey())
                .thenReturn(newStorageKey);

        when(eventFileRepository.save(any(EventFile.class)))
                .thenAnswer(invocation -> {
                    EventFile saved = invocation.getArgument(0);
                    return EventFile.builder()
                            .id(fileId)
                            .event(saved.getEvent())
                            .storageKey(saved.getStorageKey())
                            .build();
                });

        Long result = eventFileService.attachFile(eventId, multipartFile);

        assertEquals(fileId, result);

        verify(fileStorage).upload(multipartFile);
        verify(eventFileRepository).save(any(EventFile.class));
        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("기존 대표 이미지가 있으면 새 이미지로 교체한다")
    void attachFile_replaceExistingFile() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventFileRepository.findByEventId(eventId))
                .thenReturn(Optional.of(eventFile));

        when(fileStorage.upload(multipartFile))
                .thenReturn(fileStorageResult);

        when(fileStorageResult.getStorageKey())
                .thenReturn(newStorageKey);

        Long result = eventFileService.attachFile(eventId, multipartFile);

        assertEquals(fileId, result);
        assertEquals(newStorageKey, eventFile.getStorageKey());

        verify(fileStorage).upload(multipartFile);
        verify(fileStorage).delete(oldStorageKey);
        verify(eventFileRepository, never()).save(any(EventFile.class));
    }

    @Test
    @DisplayName("존재하지 않는 행사에는 파일을 등록할 수 없다")
    void attachFile_eventNotFound() {
        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventFileService.attachFile(eventId, multipartFile)
        );

        verify(fileStorage, never()).upload(any());
        verify(eventFileRepository, never()).save(any());
    }

    // =========================
    // 행사 파일 목록 조회
    // =========================

    @Test
    @DisplayName("행사에 연결된 파일 목록을 조회한다")
    void getFiles_success() {
        when(eventRepository.existsById(eventId))
                .thenReturn(true);

        when(eventFileRepository.findAllByEventId(eventId))
                .thenReturn(List.of(eventFile));

        List<EventFile> result = eventFileService.getFiles(eventId);

        assertEquals(1, result.size());
        assertEquals(oldStorageKey, result.get(0).getStorageKey());

        verify(eventFileRepository).findAllByEventId(eventId);
    }

    @Test
    @DisplayName("존재하지 않는 행사의 파일 목록은 조회할 수 없다")
    void getFiles_eventNotFound() {
        when(eventRepository.existsById(eventId))
                .thenReturn(false);

        assertThrows(
                Exception404.class,
                () -> eventFileService.getFiles(eventId)
        );

        verify(eventFileRepository, never()).findAllByEventId(anyLong());
    }

    // =========================
    // 파일 조회
    // =========================

    @Test
    @DisplayName("행사 ID와 저장 키로 파일을 조회한다")
    void getFile_success() {
        when(eventFileRepository.findByEventIdAndStorageKey(
                eventId, oldStorageKey
        )).thenReturn(Optional.of(eventFile));

        when(fileStorage.load(oldStorageKey))
                .thenReturn(resource);

        Resource result =
                eventFileService.getFile(eventId, oldStorageKey);

        assertSame(resource, result);

        verify(fileStorage).load(oldStorageKey);
    }

    @Test
    @DisplayName("연결되지 않은 파일은 조회할 수 없다")
    void getFile_notFound() {
        when(eventFileRepository.findByEventIdAndStorageKey(
                eventId, oldStorageKey
        )).thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventFileService.getFile(eventId, oldStorageKey)
        );

        verify(fileStorage, never()).load(anyString());
    }

    @Test
    @DisplayName("행사의 대표 이미지 연결 정보를 조회한다")
    void getRepresentativeFile_success() {
        when(eventFileRepository.findByEventId(eventId))
                .thenReturn(Optional.of(eventFile));

        EventFile result = eventFileService.getFile(eventId);

        assertSame(eventFile, result);
        assertEquals(oldStorageKey, result.getStorageKey());
    }

    @Test
    @DisplayName("대표 이미지가 없으면 404 예외가 발생한다")
    void getRepresentativeFile_notFound() {
        when(eventFileRepository.findByEventId(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventFileService.getFile(eventId)
        );
    }

    @Test
    @DisplayName("저장 키로 실제 파일을 불러온다")
    void loadFile_success() {
        when(fileStorage.load(oldStorageKey))
                .thenReturn(resource);

        Resource result = eventFileService.loadFile(oldStorageKey);

        assertSame(resource, result);
        verify(fileStorage).load(oldStorageKey);
    }

    // =========================
    // 파일 삭제
    // =========================

    @Test
    @DisplayName("행사에 연결된 파일과 실제 파일을 삭제한다")
    void deleteFile_success() {
        when(eventFileRepository.findByEventIdAndStorageKey(
                eventId, oldStorageKey
        )).thenReturn(Optional.of(eventFile));

        eventFileService.deleteFile(eventId, oldStorageKey);

        verify(fileStorage).delete(oldStorageKey);

        verify(eventFileRepository).deleteByEventIdAndStorageKey(
                eventId, oldStorageKey
        );
    }

    @Test
    @DisplayName("연결되지 않은 파일은 삭제할 수 없다")
    void deleteFile_notFound() {
        when(eventFileRepository.findByEventIdAndStorageKey(
                eventId, oldStorageKey
        )).thenReturn(Optional.empty());

        assertThrows(
                Exception404.class,
                () -> eventFileService.deleteFile(eventId, oldStorageKey)
        );

        verify(fileStorage, never()).delete(anyString());

        verify(eventFileRepository, never())
                .deleteByEventIdAndStorageKey(anyLong(), anyString());
    }
}
