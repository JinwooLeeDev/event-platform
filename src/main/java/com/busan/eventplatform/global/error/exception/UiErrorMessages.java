package com.busan.eventplatform.global.error.exception;

import com.busan.eventplatform.global.i18n.LocaleMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/** Localizes explicit HTTP error text without changing stored exceptions or user content. */
@Component
@RequiredArgsConstructor
public class UiErrorMessages {
    private final LocaleMessages messages;
    private static final Pattern BALANCE = Pattern.compile("잔액이 부족합니다\\.\\(현재잔액: (\\d+)원\\)");
    private static final Pattern LENGTH = Pattern.compile("(.+) 항목은 (\\d+)자 이하로 입력해주세요\\.");
    private static final Pattern REQUIRED = Pattern.compile("(.+) 항목을 입력해주세요\\.");

    public String display(String original) {
        if (original == null) return null;
        var balance = BALANCE.matcher(original);
        if (balance.matches()) return messages.get("server.balanceInsufficient", balance.group(1));
        var length = LENGTH.matcher(original);
        if (length.matches()) return messages.get("server.fieldMaxLength", LocaleMessages.display(length.group(1)), length.group(2));
        var required = REQUIRED.matcher(original);
        if (required.matches()) return messages.get("server.fieldRequired", LocaleMessages.display(required.group(1)));
        return LocaleMessages.display(original);
    }
}
