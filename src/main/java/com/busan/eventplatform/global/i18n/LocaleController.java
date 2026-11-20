package com.busan.eventplatform.global.i18n;

import com.busan.eventplatform.login.*;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;

@Controller
@RequiredArgsConstructor
public class LocaleController {
    private final LocalePreferenceService preferences;

    @PostMapping("/locale")
    public ResponseEntity<Void> change(@RequestParam String locale,
            @RequestParam(required = false) String _localeCsrf,
            @RequestParam(defaultValue = "/") String returnTo,
            HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (!LocaleSelectionState.validToken(session, _localeCsrf)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (!SupportedLocale.isSupported(locale)) return ResponseEntity.badRequest().build();
        synchronized (session) {
            Object value = session.getAttribute(LoginSession.IDENTITY);
            if (value instanceof LoginIdentity identity && identity.isUser()) {
                Object userValue = session.getAttribute(LoginSession.USER);
                if (!(userValue instanceof SessionUserDto user) || !java.util.Objects.equals(identity.getId(), user.getId())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                // The transactional proxy has committed before session/cookie mutation.
                SessionUserDto updated = preferences.change(identity.getId(), locale);
                session.setAttribute(LoginSession.USER, updated);
                LocaleSelectionState.clear(session);
            } else if (value instanceof LoginIdentity identity && identity.isCompany()) {
                LocaleSelectionState.clear(session);
            } else {
                LocaleSelectionState.select(session, locale);
            }
            LocaleCookieResolver.writeCookie(request, response, locale);
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(safeReturnTo(returnTo))).build();
    }

    public static String safeReturnTo(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//") || value.contains("\\")
                || value.chars().anyMatch(c -> c <= 32 || c == 127)) return "/";
        try {
            URI uri = URI.create(value);
            // Encoded query values are ordinary search filters. Encoded paths are unnecessary
            // for our routes and can hide protocol-relative redirects after decoding.
            return uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawPath().contains("%")
                    ? "/" : uri.toASCIIString();
        } catch (IllegalArgumentException exception) { return "/"; }
    }
}
