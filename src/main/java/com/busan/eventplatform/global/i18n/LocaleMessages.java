package com.busan.eventplatform.global.i18n;

import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class LocaleMessages {
    private final MessageSource messageSource;
    private final Map<String, String> originals = new LinkedHashMap<>();
    private final Map<String, String> sourceKeys = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> catalogs = new ConcurrentHashMap<>();
    private final Map<String, String> scripts = new ConcurrentHashMap<>();
    private static volatile LocaleMessages displaySource;

    public LocaleMessages(MessageSource messageSource) {
        this.messageSource = messageSource;
        var properties = new Properties();
        try (var reader = new InputStreamReader(new ClassPathResource("messages.properties").getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (java.io.IOException exception) { throw new IllegalStateException("Cannot load UI message catalog", exception); }
        properties.stringPropertyNames().stream().sorted().forEach(key -> {
            String original = properties.getProperty(key);
            originals.put(key, original);
            sourceKeys.putIfAbsent(original, key);
        });
    }

    @PostConstruct void registerDisplaySource() { displaySource = this; }

    public String get(String key, Object... args) { return getForLocale(key, LocaleContextHolder.getLocale(), args); }
    public String getForLocale(String key, Locale locale, Object... args) {
        String value = messageSource.getMessage(key, null, key, SupportedLocale.locale(locale.toLanguageTag()));
        if (args == null || args.length == 0) return value;
        var matcher = Pattern.compile("\\{(\\d+)\\}").matcher(value);
        var result = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(index < args.length ? String.valueOf(args[index]) : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Map<String, String> catalog(Locale locale) {
        String code = SupportedLocale.normalize(locale.toLanguageTag());
        return catalogs.computeIfAbsent(code, ignored -> {
            var result = new LinkedHashMap<String, String>();
            originals.forEach((key, original) -> result.put(key, getForLocale(key, SupportedLocale.locale(code))));
            return java.util.Collections.unmodifiableMap(result);
        });
    }

    public Map<String, Object> view(Locale locale) {
        var result = new LinkedHashMap<String, Object>();
        catalog(locale).forEach((key, value) -> {
            String[] parts = key.split("\\.");
            Map<String, Object> cursor = result;
            for (int i = 0; i < parts.length - 1; i++) {
                @SuppressWarnings("unchecked")
                var child = (Map<String, Object>) cursor.computeIfAbsent(parts[i], ignored -> new LinkedHashMap<String, Object>());
                cursor = child;
            }
            cursor.put(parts[parts.length - 1], value);
            // Keep the 1-A common aliases used by the existing language selector/header.
            if (key.startsWith("common.") && parts.length == 2) result.put(parts[1], value);
        });
        return result;
    }

    public String javascript(Locale locale) {
        String code = SupportedLocale.normalize(locale.toLanguageTag());
        return scripts.computeIfAbsent(code, ignored -> JsonMapper.builder().build().writeValueAsString(
                Map.of("locale", code, "messages", catalog(locale), "originals", originals))
                .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
                .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029"));
    }

    /** Only call for explicit UI labels/messages, never arbitrary DB or user-authored content. */
    public static String display(String original) {
        return displaySource == null ? original : displaySource.translate(original);
    }

    public static String display(String original, Locale locale) {
        if (displaySource == null || original == null) return original;
        String key = displaySource.sourceKeys.get(original);
        return key == null ? original : displaySource.getForLocale(key, locale);
    }

    public static String format(String key, Object... args) {
        return displaySource == null ? Defaults.SOURCE.getForLocale(key, Locale.KOREAN, args) : displaySource.get(key, args);
    }

    private static class Defaults { private static final LocaleMessages SOURCE = new LocaleMessages(new I18nConfig().messageSource()); }

    public String translate(String original) {
        if (original == null || original.isEmpty()) return original;
        String key = sourceKeys.get(original);
        if (key == null) return original;
        return get(key);
    }
}
