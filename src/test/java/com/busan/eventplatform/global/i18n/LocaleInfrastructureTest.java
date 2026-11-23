package com.busan.eventplatform.global.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.context.support.ResourceBundleMessageSource;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class LocaleInfrastructureTest {
    @ParameterizedTest @ValueSource(strings = {"//evil.test", "/\\evil.test", "/%2f%2fevil.test", "/%5cevil.test", "https://evil.test", "/\r\nLocation:evil", "/%252f%252fevil.test"})
    void redirectRejectsExternalAndEncodedBypasses(String path) { assertThat(LocaleController.safeReturnTo(path)).isEqualTo("/"); }

    @Test void internalRedirectPreservesEncodedSearchQuery() {
        assertThat(LocaleController.safeReturnTo("/events/list?keyword=%EB%B6%80%EC%82%B0&page=2"))
                .isEqualTo("/events/list?keyword=%EB%B6%80%EC%82%B0&page=2");
    }

    @Test void missingTranslationUsesKoreanAndUnknownKeyRemainsSafe() {
        var source = new ResourceBundleMessageSource(); source.setBasename("i18n-fallback-test");
        source.setDefaultEncoding("UTF-8"); source.setFallbackToSystemLocale(false);
        var messages = new LocaleMessages(source);
        assertThat(messages.getForLocale("fallback", Locale.ENGLISH)).isEqualTo("한국어 기본 메시지");
        assertThat(messages.getForLocale("missing.key", Locale.JAPANESE)).isEqualTo("missing.key");
    }

    @Test void resolverIgnoresBrowserLanguageAndDoesNotWriteCookieOnReads() {
        var resolver = new LocaleCookieResolver(); var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");
        assertThat(resolver.resolveLocale(request).toLanguageTag()).isEqualTo("ko");
        var response = new MockHttpServletResponse();
        resolver.setLocale(request, response, Locale.FRENCH);
        assertThat(response.getCookie("BUSAN_PLAY_LOCALE").getValue()).isEqualTo("ko");
    }
}
