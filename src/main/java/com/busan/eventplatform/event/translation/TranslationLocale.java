package com.busan.eventplatform.event.translation;

import com.busan.eventplatform.global.i18n.SupportedLocale;

public final class TranslationLocale {
    private TranslationLocale() {}
    public static String require(String locale) {
        if (!SupportedLocale.isSupported(locale))
            throw new IllegalArgumentException("Unsupported translation locale");
        return locale;
    }
}
