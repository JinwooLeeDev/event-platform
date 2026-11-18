package com.busan.eventplatform.event.event_review;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_review.event_review_like.EventReviewLike;
import com.busan.eventplatform.event.event_review.event_review_like.EventReviewLikeRepository;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    private EventSession eventSession;
    private User user;
    private EventReview eventReview;

    private final Long SESSION_ID = 1L;
    private final Long USER_ID = 10L;
    private final Long REVIEW_ID = 100L;

    @BeforeEach
    void setUp() {
        eventSession = mock(EventSession.class);
        user = mock(User.class);
        eventReview = mock(EventReview.class);
    }

    // 최신순 조회
    @Test
    void 최신순_후기목록_조회() {
        when(eventReviewRepository.findAllByEventSessionId(SESSION_ID))
                .thenReturn(List.of(eventReview));

        List<EventReview> result =
                eventReviewService.findAllByEventSessionId(SESSION_ID, "latest");

        assertEquals(1, result.size());

        verify(eventReviewRepository).findAllByEventSessionId(SESSION_ID);
        verify(eventReviewRepository, never())
                .findAllByEventSessionIdOrderByLikeCount(any());
    }

    // 좋아요순 조회
    @Test
    void 좋아요순_후기목록_조회() {
        when(eventReviewRepository.findAllByEventSessionIdOrderByLikeCount(SESSION_ID))
                .thenReturn(List.of(eventReview));

        List<EventReview> result =
                eventReviewService.findAllByEventSessionId(SESSION_ID, "like");

        assertEquals(1, result.size());

        verify(eventReviewRepository)
                .findAllByEventSessionIdOrderByLikeCount(SESSION_ID);
    }

    // 잘못된 정렬값은 최신순
    @Test
    void 잘못된_정렬값은_최신순으로_조회() {
        when(eventReviewRepository.findAllByEventSessionId(SESSION_ID))
                .thenReturn(List.of());

        eventReviewService.findAllByEventSessionId(SESSION_ID, "wrong");

        verify(eventReviewRepository).findAllByEventSessionId(SESSION_ID);
    }

    // 후기 작성 성공
    @Test
    void 후기작성_성공() {
        when(eventReviewRepository.existsReview(SESSION_ID, USER_ID))
                .thenReturn(false);
        when(eventSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(eventSession));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(eventReviewRepository.save(any(EventReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventReview result =
                eventReviewService.save(SESSION_ID, USER_ID, "좋은 행사였습니다.");

        assertEquals("좋은 행사였습니다.", result.getContent());
        assertEquals(0, result.getLikeCount());

        verify(eventReviewRepository).save(any(EventReview.class));
    }

    // 빈 내용으로 후기 작성
    @Test
    void 빈_내용으로_후기작성하면_예외발생() {
        assertThrows(Exception400.class,
                () -> eventReviewService.save(SESSION_ID, USER_ID, ""));

        verifyNoInteractions(eventSessionRepository, userRepository);
    }

    // null 내용으로 후기 작성
    @Test
    void null_내용으로_후기작성하면_예외발생() {
        assertThrows(Exception400.class,
                () -> eventReviewService.save(SESSION_ID, USER_ID, null));
    }

    // 중복 후기 작성
    @Test
    void 중복_후기작성하면_예외발생() {
        when(eventReviewRepository.existsReview(SESSION_ID, USER_ID))
                .thenReturn(true);

        assertThrows(Exception400.class,
                () -> eventReviewService.save(SESSION_ID, USER_ID, "중복 후기"));

        verify(eventSessionRepository, never()).findById(any());
    }

    // 존재하지 않는 회차
    @Test
    void 존재하지_않는_회차에_후기작성하면_예외발생() {
        when(eventReviewRepository.existsReview(SESSION_ID, USER_ID))
                .thenReturn(false);
        when(eventSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.save(SESSION_ID, USER_ID, "후기"));
    }

    // 존재하지 않는 회원
    @Test
    void 존재하지_않는_회원이_후기작성하면_예외발생() {
        when(eventReviewRepository.existsReview(SESSION_ID, USER_ID))
                .thenReturn(false);
        when(eventSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(eventSession));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.save(SESSION_ID, USER_ID, "후기"));
    }

    // 후기 수정 성공
    @Test
    void 후기수정_성공() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(eventReview.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(eventReview.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(SESSION_ID);

        Long result = eventReviewService.update(REVIEW_ID, USER_ID, "수정된 후기");

        assertEquals(SESSION_ID, result);
        verify(eventReview).updateContent("수정된 후기");
    }

    // 빈 내용으로 수정
    @Test
    void 빈_내용으로_후기수정하면_예외발생() {
        assertThrows(Exception400.class,
                () -> eventReviewService.update(REVIEW_ID, USER_ID, ""));

        verify(eventReviewRepository, never()).findByIdWithUser(any());
    }

    // 존재하지 않는 후기 수정
    @Test
    void 존재하지_않는_후기를_수정하면_예외발생() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.update(REVIEW_ID, USER_ID, "수정"));
    }

    // 다른 회원의 후기 수정
    @Test
    void 다른_회원의_후기를_수정하면_예외발생() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(eventReview.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(20L);

        assertThrows(Exception403.class,
                () -> eventReviewService.update(REVIEW_ID, USER_ID, "수정"));

        verify(eventReview, never()).updateContent(any());
    }

    // 후기 삭제 성공
    @Test
    void 후기삭제_성공_좋아요기록도_먼저삭제() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(eventReview.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(eventReview.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(SESSION_ID);

        Long result = eventReviewService.delete(REVIEW_ID, USER_ID);

        assertEquals(SESSION_ID, result);

        var order = inOrder(eventReviewLikeRepository, eventReviewRepository);
        order.verify(eventReviewLikeRepository).deleteAllByReviewId(REVIEW_ID);
        order.verify(eventReviewRepository).delete(eventReview);
    }

    // 존재하지 않는 후기 삭제
    @Test
    void 존재하지_않는_후기를_삭제하면_예외발생() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.delete(REVIEW_ID, USER_ID));

        verify(eventReviewRepository, never()).delete(any());
    }

    // 다른 회원의 후기 삭제
    @Test
    void 다른_회원의_후기를_삭제하면_예외발생() {
        when(eventReviewRepository.findByIdWithUser(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(eventReview.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(20L);

        assertThrows(Exception403.class,
                () -> eventReviewService.delete(REVIEW_ID, USER_ID));

        verify(eventReviewLikeRepository, never()).deleteAllByReviewId(any());
        verify(eventReviewRepository, never()).delete(any());
    }

    // 좋아요 등록
    @Test
    void 좋아요_등록_성공() {
        when(eventReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(eventReviewLikeRepository.findLike(REVIEW_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(eventReview.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(SESSION_ID);

        Long result = eventReviewService.toggleLike(REVIEW_ID, USER_ID);

        assertEquals(SESSION_ID, result);

        verify(eventReviewLikeRepository).save(any(EventReviewLike.class));
        verify(eventReview).increaseLikeCount();
        verify(eventReview, never()).decreaseLikeCount();
    }

    // 좋아요 취소
    @Test
    void 좋아요_취소_성공() {
        EventReviewLike like = mock(EventReviewLike.class);

        when(eventReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(eventReviewLikeRepository.findLike(REVIEW_ID, USER_ID))
                .thenReturn(Optional.of(like));
        when(eventReview.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(SESSION_ID);

        Long result = eventReviewService.toggleLike(REVIEW_ID, USER_ID);

        assertEquals(SESSION_ID, result);

        verify(eventReviewLikeRepository).delete(like);
        verify(eventReview).decreaseLikeCount();
        verify(eventReview, never()).increaseLikeCount();
    }

    // 존재하지 않는 후기 좋아요
    @Test
    void 존재하지_않는_후기에_좋아요하면_예외발생() {
        when(eventReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.toggleLike(REVIEW_ID, USER_ID));

        verify(userRepository, never()).findById(any());
    }

    // 존재하지 않는 회원 좋아요
    @Test
    void 존재하지_않는_회원이_좋아요하면_예외발생() {
        when(eventReviewRepository.findById(REVIEW_ID))
                .thenReturn(Optional.of(eventReview));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(Exception404.class,
                () -> eventReviewService.toggleLike(REVIEW_ID, USER_ID));

        verify(eventReviewLikeRepository, never()).save(any());
    }
}