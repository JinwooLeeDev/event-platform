package com.busan.eventplatform.login.mypage;

import com.busan.eventplatform.api.payment.PaymentService;
import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginType;
import com.busan.eventplatform.user.registration.RegistrationRes;
import com.busan.eventplatform.user.registration.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.samskivert.mustache.Mustache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyHistoryPageTest {
    @Test
    void usesRegistrationHistoryAndDatabaseBalanceWithEncodedTabLinks() {
        var registration = mock(RegistrationService.class);
        var myPage = mock(MyPageService.class);
        var purchases = mock(PurchaseService.class);
        var identity = LoginIdentity.builder().id(2L).loginType(LoginType.USER).build();
        var history = RegistrationRes.MyHistoryDto.builder().totalCount(3).confirmedCount(1)
                .upcomingCount(1).waitingCount(1).pastCount(1).currentTab("waiting")
                .keyword("A&B +#").items(List.of()).empty(true).build();
        when(registration.getMyHistory(identity, "waiting", "A&B +#")).thenReturn(history);
        when(myPage.getMyPage(2L)).thenReturn(MyPageResDto.builder().point(0).build());
        var controller = new MyPageController(myPage, purchases, mock(PaymentService.class), registration);
        var model = new ExtendedModelMap();
        assertEquals("user/myhistory", controller.myHistory(identity, "waiting", "A&B +#", model));
        assertSame(history, model.get("history"));
        assertEquals(0, model.get("myPoint"));
        assertEquals(0, model.get("resultCount"));
        assertTrue(model.get("historyTabs").toString().contains("keyword=A%26B+%2B%23"));
        verifyNoInteractions(purchases);
        String html = render(model);
        assertTrue(html.contains("class=\"point-value format-point\">0</strong>"));
        assertTrue(html.contains("class=\"nav-link active\" href=\"/myhistory?tab&#x3D;waiting&amp;keyword&#x3D;A%26B+%2B%23\" aria-current=\"page\""));
        assertTrue(html.contains("name=\"tab\" value=\"waiting\""));
        assertTrue(html.contains("조건에 맞는 신청 내역이 없습니다."));
        assertFalse(html.contains("fieldset disabled"));
        assertFalse(html.contains("data-history-filter"));
    }

    @Test
    void rendersServerProvidedCardValuesWithoutRecalculatingStateOrMoney() {
        var item = RegistrationRes.MyHistoryItemDto.builder().registrationId(9L).eventId(3L)
                .category("전시").title("<script>행사</script>").sessionName("오후 회차").place("부산")
                .scheduleText("서버 일정").statusLabel("대기열 등록").statusClass("status-waiting")
                .statusMessage("서버 안내").waiting(true).waitingRank(3).paidPointText("25,000P")
                .purchasedAtText("2026. 11. 12").expectedRefundText("25,000P").refundText("")
                .cancelable(true).cancelConfirmText("취소할까요?\nO'Neil \"확인\"")
                .cancelUrl("/registrations/cancel/9").detailUrl("/event/3").build();
        var history = RegistrationRes.MyHistoryDto.builder().totalCount(5).confirmedCount(2)
                .waitingCount(1).pastCount(2).currentTab("waiting").keyword("").items(List.of(item)).empty(false).build();
        var model = new ExtendedModelMap();
        model.addAttribute("history", history);
        model.addAttribute("myPoint", 1000);
        model.addAttribute("resultCount", 1);
        model.addAttribute("historyTabs", List.of());
        String html = render(model);
        assertTrue(html.contains("&lt;script&gt;행사&lt;/script&gt;"));
        assertTrue(html.contains("status-badge status-waiting"));
        assertTrue(html.contains("서버 일정"));
        assertTrue(html.contains("서버 안내"));
        assertTrue(html.contains("<dd>3번</dd>"));
        assertTrue(html.contains("25,000P"));
        assertTrue(html.contains("action=\"/registrations/cancel/9\" method=\"post\""));
        assertTrue(html.contains("confirm(this.dataset.confirm)"));
        assertFalse(html.contains("Date.parse"));
        assertFalse(html.contains("승인 대기"));
        assertFalse(html.contains("환불 완료"));
    }

    @Test
    void rejectsAnonymousAndCompanyBeforeLoadingMemberData() {
        var registration = mock(RegistrationService.class);
        var myPage = mock(MyPageService.class);
        var controller = new MyPageController(myPage, mock(PurchaseService.class), mock(PaymentService.class), registration);
        assertThrows(com.busan.eventplatform.global.error.exception.Exception401.class,
                () -> controller.myHistory(null, "all", "", new ExtendedModelMap()));
        var company = LoginIdentity.builder().id(1L).loginType(LoginType.COMPANY).build();
        assertThrows(com.busan.eventplatform.global.error.exception.Exception403.class,
                () -> controller.myHistory(company, "all", "", new ExtendedModelMap()));
        verifyNoInteractions(myPage, registration);
    }

    private String render(ExtendedModelMap model) {
        model.addAttribute("loginUser", false);
        var compiler = Mustache.compiler().withLoader(name -> new InputStreamReader(
                getClass().getResourceAsStream("/templates/" + name + ".mustache"), StandardCharsets.UTF_8));
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/templates/user/myhistory.mustache"), StandardCharsets.UTF_8)) {
            return compiler.compile(reader).execute(model);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
