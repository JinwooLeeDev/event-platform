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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventReviewService {

    private final EventReviewRepository eventReviewRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserRepository userRepository;
    private final EventReviewLikeRepository eventReviewLikeRepository;

    // 특정 회차 후기 목록 조회 (최신순 / 좋아요순)
    public List<EventReview> findAllByEventSessionId(Long eventSessionId, String sort) {
        if ("like".equals(sort)) {
            return eventReviewRepository.findAllByEventSessionIdOrderByLikeCount(eventSessionId);
        }
        return eventReviewRepository.findAllByEventSessionId(eventSessionId);
    }

    // 후기 작성
    @Transactional
    public EventReview save(Long eventSessionId, Long userId, String content) {

        if (content == null || content.isBlank()) {
            throw new Exception400("후기 내용을 입력해주세요");
        }
        boolean exists = eventReviewRepository.existsReview(eventSessionId, userId);

        if (exists) {
            throw new Exception400("이미 후기를 작성했습니다.");
        }

        // 해당 회차에 출석했는지 확인
        boolean attended = eventReviewRepository.existsAttendance(eventSessionId, userId);

        if (!attended) {
            throw new Exception403("출석 확인된 회원만 후기를 작성할 수 있습니다.");
        }

        EventSession eventSession = eventSessionRepository.findById(eventSessionId)
                .orElseThrow(() -> new Exception404("행사 회차를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("회원을 찾을 수 없습니다."));

        EventReview eventReview = EventReview.builder()
                .eventSession(eventSession)
                .user(user)
                .content(content)
                .likeCount(0)
                .build();
        return eventReviewRepository.save(eventReview);
    }

    // 후기 수정
    @Transactional
    public Long update(Long id, Long userId, String content) {

        if (content == null || content.isBlank()) {
            throw new Exception400("후기 내용을 입력해주세요");
        }

        EventReview eventReview = eventReviewRepository.findByIdWithUser(id)
                .orElseThrow(() -> new Exception404("후기를 찾을 수 없습니다."));

        if (!eventReview.getUser().getId().equals(userId)) {
            throw new Exception403("본인이 작성한 후기만 수정할 수 있습니다.");
        }

        eventReview.updateContent(content);

        return eventReview.getEventSession().getId();
    }

    // 후기 삭제
    @Transactional
    public Long delete(Long id, Long userId) {

        EventReview eventReview = eventReviewRepository.findByIdWithUser(id)
                .orElseThrow(() -> new Exception404("후기를 찾을 수 없습니다."));

        if (!eventReview.getUser().getId().equals(userId)) {
            throw new Exception403("본인이 작성한 후기만 삭제할 수 있습니다.");
        }

        Long eventSessionId = eventReview.getEventSession().getId();
        eventReviewLikeRepository.deleteAllByReviewId(id);
        eventReviewRepository.delete(eventReview);

        return eventSessionId;
    }

    // 좋아요 등록 및 취소
    @Transactional
    public Long toggleLike(Long reviewId, Long userId) {

        EventReview eventReview = eventReviewRepository.findById(reviewId)
                .orElseThrow(() -> new Exception404("후기를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("회원을 찾을 수 없습니다."));

        // 현재 회원이 해당 후기에 좋아요를 눌렀는지 확인
        var like = eventReviewLikeRepository.findLike(reviewId, userId);

        if (like.isPresent()) {
            // 이미 좋아요 눌렀다면 취소
            eventReviewLikeRepository.delete(like.get());
            eventReview.decreaseLikeCount();
        } else {
            // 좋아요 등록
            EventReviewLike eventReviewLike = EventReviewLike.builder()
                    .eventReview(eventReview)
                    .user(user)
                    .build();

            eventReviewLikeRepository.save(eventReviewLike);
            eventReview.increaseLikeCount();
        }
        return eventReview.getEventSession().getId();
    }
}
