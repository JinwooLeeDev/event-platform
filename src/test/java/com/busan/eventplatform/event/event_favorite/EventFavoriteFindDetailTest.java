
package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFavoriteFindDetailTest {

    @Mock
    private EventFavoriteRepository eventFavoriteRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventFavoriteService eventFavoriteService;


    // 로그인 회원이 좋아요를 누른 행사 조회
    @Test
    void findDetail_favorite_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(mock(Event.class)));

        when(eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId))
                .thenReturn(true);

        when(eventFavoriteRepository.countByEventId(eventId))
                .thenReturn(5L);

        // when
        EventFavoriteResponse.DetailDto result =
                eventFavoriteService.findDetail(userId, eventId);

        // then
        assertTrue(result.isFavorite());
        assertEquals(5L, result.getFavoriteCount());
    }


    // 로그인 회원이 좋아요를 누르지 않은 행사 조회
    @Test
    void findDetail_not_favorite_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(mock(Event.class)));

        when(eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId))
                .thenReturn(false);

        when(eventFavoriteRepository.countByEventId(eventId))
                .thenReturn(5L);

        // when
        EventFavoriteResponse.DetailDto result =
                eventFavoriteService.findDetail(userId, eventId);

        // then
        assertFalse(result.isFavorite());
        assertEquals(5L, result.getFavoriteCount());
    }


    // 비로그인 회원의 좋아요 개수 조회
    @Test
    void findDetail_not_login_test() {
        // given
        Long eventId = 1L;

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(mock(Event.class)));

        when(eventFavoriteRepository.countByEventId(eventId))
                .thenReturn(5L);

        // when
        EventFavoriteResponse.DetailDto result =
                eventFavoriteService.findDetail(null, eventId);

        // then
        assertFalse(result.isFavorite());
        assertEquals(5L, result.getFavoriteCount());

        verify(eventFavoriteRepository, never())
                .existsByUserIdAndEventId(any(), any());
    }


    // 존재하지 않는 행사 조회
    @Test
    void findDetail_event_not_found_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception404.class, () ->
                eventFavoriteService.findDetail(userId, eventId));

        verify(eventFavoriteRepository, never())
                .countByEventId(eventId);
    }
}
