package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EventFavoriteController {

    private final EventFavoriteService eventFavoriteService;

    // 좋아요 등록
    // POST - http://localhost:8080/events/favorite/1
    @PostMapping("/events/favorite/{eventId}")
    public ResponseEntity<EventFavoriteResponse.DetailDto> save(@PathVariable Long eventId, HttpSession session){
        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(Define.SESSION_USER);
        if (sessionUserDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } // UNAUTHORIZED : HTTP 상태 코드 401
        Long userId = sessionUserDto.getId();
        eventFavoriteService.save(userId, eventId);

        return ResponseEntity.ok(eventFavoriteService.findDetail(userId, eventId));
    }

    // 좋아요 취소
    // DELETE - http://localhost:8080/events/favorite/1
    @DeleteMapping("/events/favorite/{eventId}")
    public ResponseEntity<EventFavoriteResponse.DetailDto> delete(@PathVariable Long eventId, HttpSession session) {
        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(Define.SESSION_USER);
        if (sessionUserDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = sessionUserDto.getId();
        eventFavoriteService.delete(userId, eventId);

        return ResponseEntity.ok(eventFavoriteService.findDetail(userId, eventId));
    }

    // 좋아요 상태 및 개수 조회
    // GET - http://localhost:8080/events/favorite/1
    @GetMapping("/events/favorite/{eventId}")
    public EventFavoriteResponse.DetailDto findDetail(@PathVariable Long eventId, HttpSession session) {
        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(Define.SESSION_USER);
        Long userId = (sessionUserDto == null) ? null : sessionUserDto.getId();

        return eventFavoriteService.findDetail(userId, eventId);
    }

}
