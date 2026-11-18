//
//package com.busan.eventplatform.event.events;
//
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class EventTest {
//
//    @Test
//    void 결제대기_상태이면_true() {
//        Event event = Event.builder()
//                .status(EventStatus.PAID_READY)
//                .build();
//
//        assertTrue(event.isPaidStatus());
//    }
//
//    @Test
//    void 결제대기_상태가_아니면_false() {
//        Event event = Event.builder()
//                .status(EventStatus.PENDING_REVIEW)
//                .build();
//
//        assertFalse(event.isPaidStatus());
//    }
//
//}
