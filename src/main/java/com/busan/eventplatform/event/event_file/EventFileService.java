package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventFileService {

    private final EventRepository eventRepository;
    private final EventFileRepository eventFileRepository;

    // 행사에 파일 연결
    public Long attachFile(Long eventId, String storageKey, Integer displayOrder) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));

        EventFile eventFile = EventFile.builder()
                .event(event)
                .storageKey(storageKey)
                .displayOrder(displayOrder)
                .build();

        return eventFileRepository.save(eventFile).getId();
    }

    // 행사에 연결된 파일 목록 조회
    public List<EventFile> getFiles(Long eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new Exception404("행사를 찾을 수 없습니다.");
        }

        return eventFileRepository
                .findAllByEventIdOrderByDisplayOrder(eventId);
    }

    // 행사에 연결된 파일 삭제
    public void deleteFile(Long eventId, String storageKey) {

        EventFile eventFile = eventFileRepository
                .findByEventIdAndStorageKey(eventId, storageKey)
                .orElseThrow(() -> new Exception404("첨부파일을 찾을 수 없습니다."));

        eventFileRepository.delete(eventFile);
    }
}