package com.busan.eventplatform.point;

import com.busan.eventplatform.api.payment.*;
import com.busan.eventplatform.api.purchase.*;
import com.busan.eventplatform.api.refund.*;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.registration.RegistrationRepository;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalPointHistoryTest {
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final RegistrationRepository registrations = mock(RegistrationRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final PurchaseService service = new PurchaseService(purchases, mock(UserRepository.class),
            registrations, mock(EventSessionRepository.class), payments, refunds);

    @Test
    void combinesActualTransactionsNewestFirstWithoutNettingOriginalPurchase() {
        var purchase = purchase(10L, 10000, "2026-11-19 10:00:00");
        var refund = refund(purchase, 5000, "2026-11-19 11:00:00");
        given(List.of(purchase), List.of(refund), List.of(charge(30000, "2026-11-19 09:00:00")));

        var history = service.pointHistory(2L);

        assertEquals(List.of("환불", "행사 구매", "충전"), history.stream().map(PurchaseRes.PointHistoryDto::getTypeLabel).toList());
        assertEquals(List.of(5000, 10000, 30000), history.stream().map(PurchaseRes.PointHistoryDto::getAmount).toList());
        assertEquals("부분 환불", history.get(0).getStatusLabel());
        assertEquals("부분 환불", history.get(1).getStatusLabel());
        assertFalse(history.get(0).isDebit());
        assertTrue(history.get(1).isDebit());
        assertEquals("/event/7", history.get(0).getDetailUrl());
        assertEquals("/event/7", history.get(1).getDetailUrl());
        assertEquals(25000, history.stream().mapToInt(row -> row.isDebit() ? -row.getAmount() : row.getAmount()).sum());
        verify(payments).paymentList(2L);
        verify(refunds).findByUserIdWithPurchase(2L, RefundStatus.REFUND);
    }

    @Test
    void keepsRefundOfOldPurchaseSeparateFromReapplicationToSameSession() {
        var oldPurchase = purchase(10L, 10000, "2026-11-18 09:00:00");
        var newPurchase = purchase(11L, 10000, "2026-11-19 10:00:00");
        given(List.of(oldPurchase, newPurchase),
                List.of(refund(oldPurchase, 10000, "2026-11-18 10:00:00")), List.of());

        var history = service.pointHistory(2L);

        assertEquals("구매 완료", history.get(0).getStatusLabel());
        assertEquals("전액 환불", history.get(1).getStatusLabel());
        assertEquals("전액 환불", history.get(2).getStatusLabel());
        assertEquals(3, history.size());
    }

    @Test
    void showsZeroRefundExplicitlyForFreeAndNonRefundablePurchases() {
        var free = purchase(10L, 0, "2026-11-19 09:00:00");
        var paid = purchase(11L, 10000, "2026-11-19 09:00:00");
        given(List.of(free, paid), List.of(refund(free, 0, "2026-11-19 11:00:00"),
                refund(paid, 0, "2026-11-19 10:00:00")), List.of());

        var history = service.pointHistory(2L);

        assertEquals(4, history.size());
        assertTrue(history.stream().allMatch(row -> row.getStatusLabel().equals("환불액 0P")));
        assertEquals(0, history.get(0).getAmount());
        assertEquals(0, history.get(1).getAmount());
    }

    @Test
    void handlesNoHistoryWithoutQueryingAnEmptySessionIdList() {
        given(List.of(), List.of(), List.of());
        assertTrue(service.pointHistory(2L).isEmpty());
        verifyNoInteractions(registrations);
    }

    @Test
    void preservesEntriesWithMissingDateAtEnd() {
        given(List.of(), List.of(), List.of(charge(1000, null), charge(2000, "2026-11-19 09:00:00")));
        var history = service.pointHistory(2L);
        assertEquals(2000, history.get(0).getAmount());
        assertNull(history.get(1).getOccurredAt());
    }

    private void given(List<Purchase> purchaseList, List<Refund> refundList, List<PaymentRes.ListDto> chargeList) {
        when(purchases.findByUserIdWithPurchaseList(2L)).thenReturn(purchaseList);
        when(payments.paymentList(2L)).thenReturn(chargeList);
        when(refunds.findByUserIdWithPurchase(2L, RefundStatus.REFUND)).thenReturn(refundList);
    }

    private Purchase purchase(Long id, int price, String date) {
        var purchase = mock(Purchase.class, RETURNS_DEEP_STUBS);
        when(purchase.getId()).thenReturn(id);
        when(purchase.getPrice()).thenReturn(price);
        when(purchase.getPurchasedAt()).thenReturn(Timestamp.valueOf(date));
        EventSession session = purchase.getEventSession();
        when(session.getId()).thenReturn(4L);
        when(session.getName()).thenReturn("오후 회차");
        when(session.getStartsAt()).thenReturn(LocalDateTime.of(2026, 12, 1, 10, 0));
        when(session.getEvent().getId()).thenReturn(7L);
        when(session.getEvent().getTitle()).thenReturn("부산 행사");
        when(session.getEvent().getCategory().getName()).thenReturn("체험");
        // 현재 참가비와 무관하게 구매 당시 가격으로 환불을 구분해야 한다.
        when(session.getFee()).thenReturn(99999);
        return purchase;
    }

    private Refund refund(Purchase purchase, int amount, String date) {
        var refund = mock(Refund.class);
        when(refund.getPurchase()).thenReturn(purchase);
        when(refund.getAmount()).thenReturn(amount);
        when(refund.getCreatedAt()).thenReturn(Timestamp.valueOf(date));
        return refund;
    }

    private PaymentRes.ListDto charge(int amount, String date) {
        var payment = new Payment();
        payment.setAmount(amount);
        payment.setPaidAt(date == null ? null : Timestamp.valueOf(date));
        return new PaymentRes.ListDto(payment);
    }
}
