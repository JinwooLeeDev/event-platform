package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.global.i18n.I18nTestModels;

import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.purchase.PurchaseRes;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import com.busan.eventplatform.user.registration.RegistrationService;
import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyPointPageTest {
    private final MyPageService myPage = mock(MyPageService.class);
    private final PurchaseService purchases = mock(PurchaseService.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final MyPageController controller = new MyPageController(myPage, purchases, payments, mock(RegistrationService.class));

    @Test
    void usesSessionOwnerAndExistingBalanceAndRendersUnifiedHistory() {
        var session = SessionUserDto.builder().id(2L).build();
        when(myPage.getMyPage(2L)).thenReturn(MyPageResDto.builder().point(25000).build());
        var refund = row("환불", 5000, false, "부분 환불");
        var purchase = row("행사 구매", 10000, true, "부분 환불");
        when(purchases.pointHistory(2L)).thenReturn(List.of(refund, purchase));
        var model = new ExtendedModelMap();

        assertEquals("user/mypoint", controller.myPoint(session, model));
        assertSame(refund, ((List<?>) model.get("pointHistory")).getFirst());
        verifyNoInteractions(payments);
        var html = render(model);
        assertTrue(html.contains("전체 거래 내역"));
        assertTrue(html.contains("부분 환불"));
        assertTrue(html.contains("&lt;script&gt;부산&lt;/script&gt;"));
        assertFalse(html.contains("<script>부산</script>"));
        assertTrue(html.contains("href=\"/event/7\""));
        assertTrue(html.contains("point-number\">5000"));
        assertTrue(html.contains("point-number\">10000"));
        assertTrue(html.contains("point-number\">25000"));
        assertTrue(html.contains("account-point-amount--used"));
        assertFalse(html.contains("준비 중입니다"));
        assertFalse(html.contains("거래 후 잔액"));
    }

    @Test
    void rendersEmptyHistoryAndMissingDateWithoutBrokenLinks() {
        var model = new ExtendedModelMap();
        model.addAttribute("myPage", MyPageResDto.builder().point(0).build());
        model.addAttribute("pointHistory", List.of());
        assertTrue(render(model).contains("아직 포인트 거래 내역이 없어요."));

        var charge = row("충전", 1000, false, "충전 완료");
        charge.setOccurredAt(null);
        charge.setDetailUrl(null);
        model.addAttribute("pointHistory", List.of(charge));
        var html = render(model);
        assertTrue(html.contains("거래일 확인 중"));
        assertFalse(html.contains("href=\"null\""));
    }

    @Test
    void rejectsMissingPersonalSessionBeforeLoadingTransactions() {
        assertThrows(Exception401.class, () -> controller.myPoint(null, new ExtendedModelMap()));
        verifyNoInteractions(myPage, purchases, payments);
    }

    private PurchaseRes.PointHistoryDto row(String type, int amount, boolean debit, String status) {
        return PurchaseRes.PointHistoryDto.builder().typeLabel(type).amount(amount).debit(debit)
                .title("<script>부산</script>").sessionName("오후 회차").statusLabel(status)
                .occurredAt(Timestamp.valueOf("2026-11-19 11:00:00")).detailUrl("/event/7").build();
    }

    private String render(ExtendedModelMap model) {
        model.addAttribute("loginUser", false);
        var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), StandardCharsets.UTF_8));
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/user/mypoint.mustache"), StandardCharsets.UTF_8)) {
            return compiler.compile(reader).execute(I18nTestModels.withLocale(model));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
