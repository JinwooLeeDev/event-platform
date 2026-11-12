
package com.busan.eventplatform.event.event_favorite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFavoriteFindAllTest {

    @Mock
    private EventFavoriteRepository eventFavoriteRepository;

    @InjectMocks
    private EventFavoriteService eventFavoriteService;


    // 회원의 좋아요 목록 조회
    @Test
    void findAllByUserId_test() {
        // given
        Long userId = 1L;

        EventFavorite favorite1 = mock(EventFavorite.class);
        EventFavorite favorite2 = mock(EventFavorite.class);

        when(eventFavoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(favorite1, favorite2));

        // when
        List<EventFavoriteResponse.ListDto> result =
                eventFavoriteService.findAllByUserId(userId);

        // then
        assertEquals(2, result.size());

        verify(eventFavoriteRepository)
                .findAllByUserIdOrderByCreatedAtDesc(userId);
    }


    // 좋아요 목록이 비어 있을 때
    @Test
    void findAllByUserId_empty_test() {
        // given
        Long userId = 1L;

        when(eventFavoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        // when
        List<EventFavoriteResponse.ListDto> result =
                eventFavoriteService.findAllByUserId(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
