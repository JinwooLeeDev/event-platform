
package com.busan.eventplatform.event.event_file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/event-files")
public class EventFileController {

    private final EventFileService eventFileService;

    // 행사 대표 이미지 조회
    @GetMapping("/{eventId}")
    public ResponseEntity<Resource> getFile(@PathVariable Long eventId) {

        EventFile eventFile = eventFileService.getFile(eventId);

        Resource resource = eventFileService.loadFile(eventFile.getStorageKey());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
