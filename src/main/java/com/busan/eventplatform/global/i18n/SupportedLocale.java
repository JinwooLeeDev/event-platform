package com.busan.eventplatform.global.i18n;

import java.util.Locale;
import java.util.Set;

public final class SupportedLocale {
    public static final String DEFAULT = "ko";
    public static final Set<String> CODES = Set.of("ko", "en", "ja", "zh-CN");
    private SupportedLocale() { }
    public static boolean isSupported(String code) { return code != null && CODES.contains(code); }
    public static String normalize(String code) { return isSupported(code) ? code : DEFAULT; }
    public static Locale locale(String code) { return Locale.forLanguageTag(normalize(code)); }
}
