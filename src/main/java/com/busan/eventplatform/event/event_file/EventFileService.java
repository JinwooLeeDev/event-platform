package com.busan.eventplatform.event.event_file;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.global.file.FileStorageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventFileService {

    private final EventRepository eventRepository;
    private final EventFileRepository eventFileRepository;
    private final FileStorage fileStorage;

    // 행사에 파일 업로드&연결
    public Long attachFile(Long eventId, MultipartFile file) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));

        // 기존 대표 이미지 조회
        Optional<EventFile> existingFile = eventFileRepository.findByEventId(eventId);
        // 새 이미지 저장
        FileStorageResult result = fileStorage.upload(file);
        String newStorageKey = result.getStorageKey();

        // 기존 이미지가 있으면 저장 키 교체
        if (existingFile.isPresent()) {
            EventFile eventFile = existingFile.get();
            String oldStorageKey = eventFile.getStorageKey();
            eventFile.changeStorageKey(newStorageKey);
            // 기존 실제 파일 삭제
            fileStorage.delete(oldStorageKey);

            return eventFile.getId();
        }

        // 기존 이미지가 없으면 새로 연결
        EventFile eventFile = EventFile.builder()
                .event(event)
                .storageKey(newStorageKey)
                .build();

        return eventFileRepository.save(eventFile).getId();
    }

    // 행사에 연결된 파일 목록 조회
    @Transactional(readOnly = true)
    public List<EventFile> getFiles(Long eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new Exception404("행사를 찾을 수 없습니다.");
        }

        return eventFileRepository
                .findAllByEventId(eventId);
    }

    // 파일 조회
    public Resource getFile(Long eventId, String storageKey) {
        EventFile eventFile = eventFileRepository
                .findByEventIdAndStorageKey(eventId, storageKey)
                .orElseThrow(() -> new Exception404("첨부파일을 찾을 수 없습니다."));

        return fileStorage.load(eventFile.getStorageKey());
    }

    // 행사에 연결된 파일 삭제
    public void deleteFile(Long eventId, String storageKey) {

        EventFile eventFile = eventFileRepository
                .findByEventIdAndStorageKey(eventId, storageKey)
                .orElseThrow(() -> new Exception404("첨부파일을 찾을 수 없습니다."));
        fileStorage.delete(eventFile.getStorageKey());
        eventFileRepository.deleteByEventIdAndStorageKey(eventId,storageKey);
    }
}