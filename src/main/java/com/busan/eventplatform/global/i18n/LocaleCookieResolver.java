package com.busan.eventplatform.global.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.servlet.LocaleResolver;
import java.time.Duration;
import java.util.Locale;

public class LocaleCookieResolver implements LocaleResolver {
    public static final String COOKIE_NAME = "BUSAN_PLAY_LOCALE";
    private static final String RESOLVED = LocaleCookieResolver.class.getName() + ".resolved";

    public static String readCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName()) && SupportedLocale.isSupported(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void writeCookie(HttpServletRequest request, HttpServletResponse response, String code) {
        if (!SupportedLocale.isSupported(code)) throw new IllegalArgumentException("Unsupported locale");
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_NAME, code)
                .path("/").sameSite("Lax").httpOnly(true).secure(request.isSecure())
                .maxAge(Duration.ofDays(365)).build().toString());
        request.setAttribute(RESOLVED, code);
    }

    @Override public Locale resolveLocale(HttpServletRequest request) {
        Object resolved = request.getAttribute(RESOLVED);
        return SupportedLocale.locale(resolved instanceof String code ? code : readCookie(request));
    }

    @Override public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (response == null) throw new IllegalArgumentException("Response required");
        writeCookie(request, response, SupportedLocale.normalize(locale == null ? null : locale.toLanguageTag()));
    }
}
