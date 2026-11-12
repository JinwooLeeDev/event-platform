
package com.busan.eventplatform.event.event_favorite;

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
class EventFavoriteDeleteTest {

    @Mock
    private EventFavoriteRepository eventFavoriteRepository;

    @InjectMocks
    private EventFavoriteService eventFavoriteService;


    // 좋아요 취소 성공
    @Test
    void delete_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        EventFavorite favorite = mock(EventFavorite.class);

        when(eventFavoriteRepository.findByUserIdAndEventId(userId, eventId))
                .thenReturn(Optional.of(favorite));

        // when
        eventFavoriteService.delete(userId, eventId);

        // then
        verify(eventFavoriteRepository).delete(favorite);
    }


    // 좋아요 정보가 존재하지 않을 때
    @Test
    void delete_not_found_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(eventFavoriteRepository.findByUserIdAndEventId(userId, eventId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception404.class, () ->
                eventFavoriteService.delete(userId, eventId));

        verify(eventFavoriteRepository, never())
                .delete(any(EventFavorite.class));
    }
}
