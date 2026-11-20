
package com.busan.eventplatform.event.eventReview;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventReview.eventReviewLike.EventReviewLike;
import com.busan.eventplatform.event.eventReview.eventReviewLike.EventReviewLikeRepository;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
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
class EventReviewServiceTest {

    @Mock
    private EventReviewRepository eventReviewRepository;

    @Mock
    private EventSessionRepository eventSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventReviewLikeRepository eventReviewLikeRepository;

    @InjectMocks
    private EventReviewService eventReviewService;

    // 출석한 회원은 후기 작성 성공
    @Test
    void save_success_when_attended() {
        Long eventSessionId = 1L;
        Long userId = 1L;
        String content = "즐거운 행사였습니다.";

        EventSession eventSession = mock(EventSession.class);
        User user = mock(User.class);

        when(eventReviewRepository.existsReview(eventSessionId, userId))
                .thenReturn(false);
        when(eventReviewRepository.existsAttendance(eventSessionId, userId))
                .thenReturn(true);
        when(eventSessionRepository.findById(eventSessionId))
                .thenReturn(Optional.of(eventSession));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(eventReviewRepository.save(any(EventReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventReview result = eventReviewService.save(
                eventSessionId, userId, content
        );

        assertNotNull(result);
        assertEquals(content, result.getContent());

        verify(eventReviewRepository).save(any(EventReview.class));
    }

    // 출석하지 않은 회원은 후기 작성 실패
    @Test
    void save_fail_when_not_attended() {
        Long eventSessionId = 1L;
        Long userId = 1L;

        when(eventReviewRepository.existsReview(eventSessionId, userId))
                .thenReturn(false);
        when(eventReviewRepository.existsAttendance(eventSessionId, userId))
                .thenReturn(false);

        assertThrows(Exception403.class, () ->
                eventReviewService.save(eventSessionId, userId, "후기 내용")
        );

        verify(eventReviewRepository, never()).save(any(EventReview.class));
    }

    // 빈 내용은 후기 작성 실패
    @Test
    void save_fail_when_content_empty() {
        assertThrows(Exception400.class, () ->
                eventReviewService.save(1L, 1L, " ")
        );

        verify(eventReviewRepository, never()).save(any(EventReview.class));
    }

    // 이미 후기를 작성한 회원은 중복 작성 실패
    @Test
    void save_fail_when_already_reviewed() {
        when(eventReviewRepository.existsReview(1L, 1L))
                .thenReturn(true);

        assertThrows(Exception400.class, () ->
                eventReviewService.save(1L, 1L, "후기 내용")
        );

        verify(eventReviewRepository, never()).save(any(EventReview.class));
    }

    // 본인이 작성한 후기 수정 성공
    @Test
    void update_success() {
        Long reviewId = 1L;
        Long userId = 1L;
        String content = "수정한 후기입니다.";

        EventReview review = mock(EventReview.class);
        User user = mock(User.class);
        EventSession eventSession = mock(EventSession.class);

        when(eventReviewRepository.findByIdWithUser(reviewId))
                .thenReturn(Optional.of(review));
        when(review.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(review.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(10L);

        Long result = eventReviewService.update(reviewId, userId, content);

        assertEquals(10L, result);
        verify(review).updateContent(content);
    }

    // 다른 회원의 후기는 수정 실패
    @Test
    void update_fail_when_not_owner() {
        EventReview review = mock(EventReview.class);
        User user = mock(User.class);

        when(eventReviewRepository.findByIdWithUser(1L))
                .thenReturn(Optional.of(review));
        when(review.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);

        assertThrows(Exception403.class, () ->
                eventReviewService.update(1L, 1L, "수정 내용")
        );
    }

    // 후기 삭제 성공
    @Test
    void delete_success() {
        EventReview review = mock(EventReview.class);
        User user = mock(User.class);
        EventSession eventSession = mock(EventSession.class);

        when(eventReviewRepository.findByIdWithUser(1L))
                .thenReturn(Optional.of(review));
        when(review.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(review.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(10L);

        Long result = eventReviewService.delete(1L, 1L);

        assertEquals(10L, result);
        verify(eventReviewLikeRepository).deleteAllByReviewId(1L);
        verify(eventReviewRepository).delete(review);
    }

    // 후기 좋아요 등록
    @Test
    void toggleLike_add_like() {
        EventReview review = mock(EventReview.class);
        User user = mock(User.class);
        EventReviewLikeRepository likeRepository = eventReviewLikeRepository;

        EventSession eventSession = mock(EventSession.class);

        when(eventReviewRepository.findById(1L))
                .thenReturn(Optional.of(review));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(likeRepository.findLike(1L, 1L))
                .thenReturn(Optional.empty());
        when(review.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(10L);

        Long result = eventReviewService.toggleLike(1L, 1L);

        assertEquals(10L, result);
        verify(likeRepository).save(any());
        verify(review).increaseLikeCount();
    }

    // 이미 누른 좋아요 취소
    @Test
    void toggleLike_remove_like() {
        EventReview review = mock(EventReview.class);
        EventReviewLike like = mock(EventReviewLike.class);
        EventSession eventSession = mock(EventSession.class);

        when(eventReviewRepository.findById(1L))
                .thenReturn(Optional.of(review));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mock(User.class)));
        when(eventReviewLikeRepository.findLike(1L, 1L))
                .thenReturn(Optional.of(like));
        when(review.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(10L);

        Long result = eventReviewService.toggleLike(1L, 1L);

        assertEquals(10L, result);
        verify(eventReviewLikeRepository).delete(like);
        verify(review).decreaseLikeCount();
    }
}
