package com.busan.eventplatform.event.events;

import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminEventReviewTest {
    @Mock EventRepository repository;
    @InjectMocks EventService service;

    @Test void blankFeedbackCannotChangePendingEvent() {
        Event event = Event.builder().status(EventStatus.PENDING_REVIEW).build();
        lenient().when(repository.findById(1L)).thenReturn(Optional.of(event));
        assertThrows(Exception400.class, () -> service.requestFeedback(1L, "   "));
        assertEquals(EventStatus.PENDING_REVIEW, event.getStatus());
    }

    @Test void approvalMovesToPaymentWaitingAndCannotBeRepeated() {
        Event event = Event.builder().status(EventStatus.PENDING_REVIEW).build();
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        service.approve(1L);
        assertEquals(EventStatus.PAID_READY, event.getStatus());
        assertThrows(Exception400.class, () -> service.approve(1L));
    }

    @Test void feedbackStoresReasonAndCannotBeRepeated() {
        Event event = Event.builder().status(EventStatus.PENDING_REVIEW).build();
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        service.requestFeedback(1L, "  Update schedule  ");
        assertEquals(EventStatus.FEEDBACK_REQUIRED, event.getStatus());
        assertEquals("Update schedule", event.getFeedbackMessage());
        assertThrows(Exception400.class, () -> service.requestFeedback(1L, "Other reason"));
    }

    @Test void oversizedFeedbackCannotChangePendingEvent() {
        assertThrows(Exception400.class, () -> service.requestFeedback(1L, "a".repeat(1001)));
        verifyNoInteractions(repository);
    }
}
