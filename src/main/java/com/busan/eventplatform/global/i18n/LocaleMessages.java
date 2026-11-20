package com.busan.eventplatform.global.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LocaleMessages {
    private final MessageSource messageSource;
    public String get(String key, Object... args) { return getForLocale(key, LocaleContextHolder.getLocale(), args); }
    public String getForLocale(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, SupportedLocale.locale(locale.toLanguageTag()));
    }
}
