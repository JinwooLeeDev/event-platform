package com.busan.eventplatform.global.i18n;

import com.busan.eventplatform.global.error.exception.GlobalExceptionHandler;
import com.busan.eventplatform.global.error.exception.UiErrorMessages;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServerUiMessagesTest {
    @Test void dynamicBalanceUsesACompleteMessageAndKeepsTheAmount() {
        LocaleMessages messages = mock(LocaleMessages.class);
        when(messages.get("server.balanceInsufficient", "12345")).thenReturn("Insufficient balance. (Current balance: 12345 KRW)");
        assertThat(new UiErrorMessages(messages).display("잔액이 부족합니다.(현재잔액: 12345원)"))
                .isEqualTo("Insufficient balance. (Current balance: 12345 KRW)");
    }

    @Test void unknownErrorTextPassesThroughWithoutPatternReplacement() {
        LocaleMessages messages = mock(LocaleMessages.class);
        String original = "User content: 잔액이 부족합니다.(현재잔액: 12345원)";
        assertThat(new UiErrorMessages(messages).display(original)).isEqualTo(original);
        verifyNoInteractions(messages);
    }

    @Test void alertStringsAreJsonEncodedAndCannotCloseTheScriptElement() {
        String original = "A user's \"message\" </script><script>alert(1)</script>\n\u2028\u2029";
        String encoded = ReflectionTestUtils.invokeMethod(GlobalExceptionHandler.class, "javascriptString", original);
        assertThat(encoded).doesNotContain("<", ">", "\u2028", "\u2029");
        assertThat(JsonMapper.builder().build().readValue(encoded, String.class)).isEqualTo(original);
    }
}
