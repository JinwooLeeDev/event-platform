package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventFavoriteService {

    private final EventFavoriteRepository eventFavoriteRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    // 좋아요 등록
    @Transactional
    public EventFavorite save(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("회원을 찾을 수 없습니다."));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));

        if (eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new Exception400("이미 좋아요를 누른 행사입니다.");
        }
        EventFavorite eventFavorite = EventFavorite.builder()
                .user(user)
                .event(event)
                .build();

        return eventFavoriteRepository.save(eventFavorite);
    }

    // 좋아요 취소
    @Transactional
    public void delete(Long userId, Long eventId) {
        EventFavorite eventFavorite = eventFavoriteRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new Exception404("좋아요 정보를 찾을 수 없습니다."));
        eventFavoriteRepository.delete(eventFavorite);
    }

    // 좋아요 상태 및 개수 조회
    public EventFavoriteResponse.DetailDto findDetail(Long userId, Long eventId) {
        boolean isFavorite = false; // 누르지 않은 상태로 시작 -- 로그인하지 않은 방문자는 좋아요 여부 확인할 수 X
        if (userId != null) {
            isFavorite = eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId);
        }
        eventRepository.findById(eventId)
                .orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        Long favoriteCount = eventFavoriteRepository.countByEventId(eventId);

        return new EventFavoriteResponse.DetailDto(isFavorite, favoriteCount);
    }


    // 회원의 좋아요 목록 조회
    public List<EventFavoriteResponse.ListDto> findAllByUserId(Long userId) {
        List<EventFavorite> favoriteList = eventFavoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return favoriteList.stream()
                .map(EventFavoriteResponse.ListDto::new)
                .toList();
    }
}
