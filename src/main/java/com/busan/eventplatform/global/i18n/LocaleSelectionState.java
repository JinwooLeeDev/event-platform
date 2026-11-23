package com.busan.eventplatform.global.i18n;

import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LocaleSelectionState {
    private static final String CHOICE = "BUSAN_PLAY_LOCALE_CHOICE";
    private static final String CSRF = "BUSAN_PLAY_LOCALE_CSRF";
    private LocaleSelectionState() { }
    public static String token(HttpSession session) {
        synchronized (session) {
            if (!(session.getAttribute(CSRF) instanceof String)) session.setAttribute(CSRF, UUID.randomUUID().toString());
            return (String) session.getAttribute(CSRF);
        }
    }
    public static boolean validToken(HttpSession session, String token) {
        if (session == null || token == null || !(session.getAttribute(CSRF) instanceof String expected)) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
    public static String choice(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(CHOICE);
        return value instanceof String code && SupportedLocale.isSupported(code) ? code : null;
    }
    public static void select(HttpSession session, String code) {
        if (!SupportedLocale.isSupported(code)) throw new IllegalArgumentException("Unsupported locale");
        session.setAttribute(CHOICE, code);
    }
    public static void clear(HttpSession session) { if (session != null) session.removeAttribute(CHOICE); }
}
