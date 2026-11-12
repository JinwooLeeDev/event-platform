
package com.busan.eventplatform.event.event_favorite;

import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFavoriteSaveTest {

    @Mock
    private EventFavoriteRepository eventFavoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventFavoriteService eventFavoriteService;


    // 좋아요 등록 성공
    @Test
    void save_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        User user = mock(User.class);
        Event event = mock(Event.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId))
                .thenReturn(false);

        when(eventFavoriteRepository.save(any(EventFavorite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        EventFavorite result = eventFavoriteService.save(userId, eventId);

        // then
        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(event, result.getEvent());

        verify(eventFavoriteRepository).save(any(EventFavorite.class));
    }


    // 이미 좋아요한 행사 등록 시 예외
    @Test
    void save_duplicate_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(mock(User.class)));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(mock(Event.class)));

        when(eventFavoriteRepository.existsByUserIdAndEventId(userId, eventId))
                .thenReturn(true);

        // when & then
        assertThrows(Exception400.class, () ->
                eventFavoriteService.save(userId, eventId));

        verify(eventFavoriteRepository, never())
                .save(any(EventFavorite.class));
    }


    // 회원이 존재하지 않을 때
    @Test
    void save_user_not_found_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception404.class, () ->
                eventFavoriteService.save(userId, eventId));

        verify(eventFavoriteRepository, never())
                .save(any(EventFavorite.class));
    }


    // 행사가 존재하지 않을 때
    @Test
    void save_event_not_found_test() {
        // given
        Long userId = 1L;
        Long eventId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(mock(User.class)));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception404.class, () ->
                eventFavoriteService.save(userId, eventId));

        verify(eventFavoriteRepository, never())
                .save(any(EventFavorite.class));
    }
}
