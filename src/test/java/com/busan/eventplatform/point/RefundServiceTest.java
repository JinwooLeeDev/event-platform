package com.busan.eventplatform.point;


import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.api.purchase.PurchaseRepository;
import com.busan.eventplatform.api.purchase.PurchaseStatus;
import com.busan.eventplatform.api.refund.Refund;
import com.busan.eventplatform.api.refund.RefundRepository;
import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.api.refund.RefundStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.registration.Registration;
import com.busan.eventplatform.user.registration.RegistrationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private Registration registration;

    @Mock
    private EventSession eventSession;

    @Mock
    private User user;

    @InjectMocks
    private RefundService refundService;

    private Purchase purchase;

    @BeforeEach
    void setUp() {
        // 실제 Purchase 객체 생성
        purchase = Purchase.builder()
                .user(user)
                .eventSession(eventSession)
                .price(6000)
                .build();

        purchase.setId(100L);
    }

    @Test
    void 정상적인_환불요청이면_포인트반환과_상태변경() {

        // given: 환불 가능한 신청과 구매가 존재
        when(registration.getEventSession()).thenReturn(eventSession);
        when(registration.getPurchase()).thenReturn(purchase);
        when(registration.getUser()).thenReturn(user);
        when(registration.getStatus()).thenReturn(
                RegistrationStatus.CANCELLED
        );

        when(eventSession.getId()).thenReturn(10L);
        when(eventSession.getStatus()).thenReturn(
                EventSessionStatus.SCHEDULED
        );
        when(eventSession.getFee()).thenReturn(6000);
        when(eventSession.getStartsAt()).thenReturn(
                LocalDate.now().plusDays(5).atTime(18, 0)
        );

        when(purchaseRepository.existsEventByUser(1L, 10L))
                .thenReturn(true);

        when(purchaseRepository.findById(100L))
                .thenReturn(Optional.of(purchase));

        // when: 행사 5일 전, 확정 상태였던 신청을 환불
        refundService.refundUser(
                1L,
                registration,
                RegistrationStatus.CONFIRMED
        );

        // then: 유저에게 6000P 반환
        verify(user).addPoint(6000);

        // Purchase 상태 변경 확인
        assertEquals(
                PurchaseStatus.CANCELED,
                purchase.getStatus()
        );

        // Refund 저장 여부와 저장 내용 확인
        ArgumentCaptor<Refund> captor =
                ArgumentCaptor.forClass(Refund.class);

        verify(refundRepository).save(captor.capture());

        Refund savedRefund = captor.getValue();

        assertEquals(6000, savedRefund.getAmount());
        assertEquals(RefundStatus.REFUND, savedRefund.getStatus());
        assertSame(purchase, savedRefund.getPurchase());
        assertSame(user, savedRefund.getUser());
    }

    @Test
    void 이미_환불된_구매는_중복환불_불가() {

        // given: 구매가 이미 취소됨
        purchase.setStatus(PurchaseStatus.CANCELED);

        when(registration.getEventSession()).thenReturn(eventSession);
        when(registration.getPurchase()).thenReturn(purchase);
        when(registration.getStatus()).thenReturn(
                RegistrationStatus.CANCELLED
        );

        when(eventSession.getId()).thenReturn(10L);
        when(eventSession.getStatus()).thenReturn(
                EventSessionStatus.SCHEDULED
        );

        when(purchaseRepository.existsEventByUser(1L, 10L))
                .thenReturn(true);

        when(purchaseRepository.findById(100L))
                .thenReturn(Optional.of(purchase));

        // when & then: 예외 발생
        assertThrows(Exception400.class, () ->
                refundService.refundUser(
                        1L,
                        registration,
                        RegistrationStatus.CONFIRMED
                )
        );

        // 포인트 반환이나 환불 저장이 없어야 함
        verify(user, never()).addPoint(anyInt());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void 구매이력이_없으면_환불불가() {

        // given: 구매 기록 없음
        when(registration.getEventSession()).thenReturn(eventSession);
        when(eventSession.getId()).thenReturn(10L);

        when(purchaseRepository.existsEventByUser(1L, 10L))
                .thenReturn(false);

        // when & then
        assertThrows(Exception400.class, () ->
                refundService.refundUser(
                        1L,
                        registration,
                        RegistrationStatus.CONFIRMED
                )
        );

        verify(user, never()).addPoint(anyInt());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void 구매금액과_행사참가비가_다르면_환불불가() {

        // given
        when(registration.getEventSession()).thenReturn(eventSession);
        when(registration.getPurchase()).thenReturn(purchase);
        when(registration.getStatus()).thenReturn(
                RegistrationStatus.CANCELLED
        );

        when(eventSession.getId()).thenReturn(10L);
        when(eventSession.getStatus()).thenReturn(
                EventSessionStatus.SCHEDULED
        );

        // 결제한 금액: 6000P
        // 현재 참가비: 9000P
        when(eventSession.getFee()).thenReturn(9000);

        when(purchaseRepository.existsEventByUser(1L, 10L))
                .thenReturn(true);

        when(purchaseRepository.findById(100L))
                .thenReturn(Optional.of(purchase));

        // when & then
        assertThrows(Exception400.class, () ->
                refundService.refundUser(
                        1L,
                        registration,
                        RegistrationStatus.CONFIRMED
                )
        );

        verify(user, never()).addPoint(anyInt());
        verifyNoInteractions(refundRepository);

        // 기존 구매 상태 유지
        assertEquals(
                PurchaseStatus.PURCHASED,
                purchase.getStatus()
        );
    }
}