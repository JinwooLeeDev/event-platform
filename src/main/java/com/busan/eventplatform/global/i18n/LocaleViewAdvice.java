package com.busan.eventplatform.global.i18n;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class LocaleViewAdvice {
    private final LocaleMessages messages;
    private final org.springframework.web.servlet.LocaleResolver localeResolver;
    private static final List<String> KEYS = List.of("language", "chooseLanguage", "languageIntro", "apply", "close",
            "search", "searchName", "searchPlaceholder", "mypage", "companyPage", "adminPage", "login", "signup", "logout",
            "terms", "privacy", "address", "businessNumber", "representative", "copyrightWarning");

    @ModelAttribute
    public void addLocale(HttpServletRequest request, Model model) {
        Object original = request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI);
        String path = original instanceof String uri ? uri : request.getRequestURI();
        path = path.substring(Math.min(request.getContextPath().length(), path.length()));
        String accept = request.getHeader("Accept");
        if (path.equals("/api") || path.startsWith("/api/")
                || path.matches("(?i).+\\.(css|js|map|png|jpe?g|gif|svg|ico|webp|avif|woff2?|ttf|eot)$")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/fonts/")
                || path.startsWith("/webjars/") || path.startsWith("/uploads/")
                || (accept != null && accept.contains("application/json") && !accept.contains("text/html"))) return;
        var locale = localeResolver.resolveLocale(request);
        String code = locale.toLanguageTag();
        Map<String, String> translations = new LinkedHashMap<>();
        for (String key : KEYS) translations.put(key, messages.getForLocale("common." + key, locale));
        model.addAttribute("i18n", translations);
        model.addAttribute("localeCode", code);
        model.addAttribute("localeFirstVisit", LocaleCookieResolver.readCookie(request) == null);
        model.addAttribute("localeCsrf", LocaleSelectionState.token(request.getSession()));
        String query = request.getQueryString();
        String returnTo = request.getRequestURI() + (query == null ? "" : "?" + query);
        model.addAttribute("localeReturnTo", LocaleController.safeReturnTo(returnTo));
        model.addAttribute("localeOptions", List.of(option("ko", "한국어", code), option("en", "English", code),
                option("ja", "日本語", code), option("zh-CN", "简体中文", code)));
    }

    private Map<String, Object> option(String code, String label, String current) {
        return Map.of("code", code, "label", label, "selected", code.equals(current));
    }
}
