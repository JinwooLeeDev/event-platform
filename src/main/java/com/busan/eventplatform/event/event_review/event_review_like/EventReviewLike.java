package com.busan.eventplatform.event.event_review.event_review_like;

import com.busan.eventplatform.event.event_review.EventReview;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_review_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_review_id", "user_id"}))
public class EventReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_review_id", nullable = false)
    private EventReview eventReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
