package com.busan.eventplatform.global.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class StaticUiTranslationTest {
    private final Object previousDisplaySource = displaySource();

    @AfterEach void restoreLocaleAndDisplaySource() throws Exception {
        LocaleContextHolder.resetLocaleContext();
        var field = LocaleMessages.class.getDeclaredField("displaySource");
        field.setAccessible(true);
        field.set(null, previousDisplaySource);
    }

    @ParameterizedTest @ValueSource(strings = {"ko", "en", "ja", "zh-CN"})
    void eachRequestLocaleHasCompleteCatalogAndNestedView(String code) throws Exception {
        var messages = new LocaleMessages(new I18nConfig().messageSource());
        Locale locale = SupportedLocale.locale(code);
        LocaleContextHolder.setLocale(locale);
        var base = properties("messages.properties");
        var translated = properties(code.equals("ko") ? "messages.properties" : "messages_" + code.replace('-', '_') + ".properties");
        assertThat(translated.stringPropertyNames()).containsExactlyInAnyOrderElementsOf(base.stringPropertyNames());
        assertThat(base.stringPropertyNames()).contains("event.ui0001", "common.ui0056", "event.ui0301");
        assertThat(messages.catalog(locale)).hasSize(base.size());
        assertThat(messages.get("common.search")).isEqualTo(translated.getProperty("common.search"));
        Map<String, Object> view = messages.view(locale);
        assertThat(view.get("search")).isEqualTo(translated.getProperty("common.search"));
        assertThat(((Map<?, ?>) view.get("event")).get("ui0001")).isEqualTo(translated.getProperty("event.ui0001"));
        for (String key : base.stringPropertyNames()) {
            String value = translated.getProperty(key);
            if (key.equals("common.ui0043") && (code.equals("en") || code.equals("zh-CN"))) continue;
            assertThat(value).as("%s %s", code, key).isNotBlank();
        }
    }

    @Test void unsupportedLocaleFallsBackToKorean() {
        var messages = new LocaleMessages(new I18nConfig().messageSource());
        assertThat(messages.catalog(Locale.FRENCH)).isEqualTo(messages.catalog(Locale.KOREAN));
        assertThat(messages.view(Locale.FRENCH)).isEqualTo(messages.view(Locale.KOREAN));
        assertThat(messages.getForLocale("common.search", Locale.FRENCH)).isEqualTo(messages.getForLocale("common.search", Locale.KOREAN));
    }

    @Test void javascriptCatalogEscapesScriptTerminatorsAndPreservesExactText() {
        String payload = "</script><script>alert('x')</script>&\"\n\u2028\u2029 {0}";
        var source = new StaticMessageSource();
        source.addMessage("common.search", Locale.ENGLISH, payload);
        var messages = new LocaleMessages(source);
        String json = messages.javascript(Locale.ENGLISH);
        assertThat(json).doesNotContain("<", ">", "&", "\u2028", "\u2029");
        assertThat(json).contains("\\u003c", "\\u003e", "\\u0026", "\\u2028", "\\u2029");
        var parsed = JsonMapper.builder().build().readTree(json);
        assertThat(parsed.get("messages").get("common.search").asText()).isEqualTo(payload);
        assertThat(parsed.get("locale").asText()).isEqualTo("en");
    }

    @Test void numericPlaceholdersPreserveQuotesNewlinesAndArgumentText() {
        var source = new StaticMessageSource();
        source.addMessage("test.format", Locale.ENGLISH, "It's \"{0}\"\n{1} / {0} / {2}");
        var messages = new LocaleMessages(source);
        assertThat(messages.getForLocale("test.format", Locale.ENGLISH, "$\\{1}", "second"))
                .isEqualTo("It's \"$\\{1}\"\nsecond / $\\{1} / {2}");
    }

    @Test void explicitSourceMappingsTranslateButUnknownDynamicContentIsUntouched() throws Exception {
        var source = new StaticMessageSource();
        source.addMessage("common.search", Locale.ENGLISH, "Search events");
        var messages = new LocaleMessages(source);
        messages.registerDisplaySource();
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(LocaleMessages.display(properties("messages.properties").getProperty("common.search"))).isEqualTo("Search events");
        String userContent = "사용자가 직접 작성한 행사 제목 <b>{0}</b>";
        assertThat(LocaleMessages.display(userContent)).isSameAs(userContent);
        assertThat(LocaleMessages.display(null)).isNull();
        assertThat(LocaleMessages.display("")).isEmpty();
        assertThat(LocaleMessages.format("common.search")).isEqualTo("Search events");
    }

    private static Object displaySource() {
        try {
            var field = LocaleMessages.class.getDeclaredField("displaySource");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static Properties properties(String filename) throws Exception {
        var result = new Properties();
        try (var input = StaticUiTranslationTest.class.getClassLoader().getResourceAsStream(filename)) {
            assertThat(input).as(filename).isNotNull();
            result.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return result;
    }
}
