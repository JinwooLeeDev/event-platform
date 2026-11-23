package com.busan.eventplatform.event.translation;

import java.util.List;
import java.util.regex.Pattern;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;

public final class TranslationValidator {
    private TranslationValidator() {}
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PROTECTED = Pattern.compile("\\$\\{[^{}]+}|\\{\\{[^{}]+}}|\\{[^{}]+}|%(?:\\d+\\$)?[sdif]|https?://[^\\s<>\"']+|\\d+(?:[.,:/-]\\d+)*");
    public static void validate(SourceContent source, TranslatedContent result, int maxOutput) {
        if (result == null) throw new TranslationFailure(INVALID_RESPONSE);
        var originals = List.of(source.title(), source.summary(), source.description());
        String[] output = {result.title(), result.summary(), result.description()};
        int total = 0;
        for (int i = 0; i < output.length; i++) {
            String text = output[i];
            if (text == null || text.isBlank()) throw new TranslationFailure(INVALID_RESPONSE);
            total += text.length();
            if (!tokens(TAG, originals.get(i), false).equals(tokens(TAG, text, false))
                    || !tokens(PROTECTED, originals.get(i), true).equals(tokens(PROTECTED, text, true)))
                throw new TranslationFailure(INVALID_RESPONSE);
        }
        if (result.title().length() > 1024 || result.summary().length() > 4096 || total > maxOutput)
            throw new TranslationFailure(INVALID_RESPONSE);
    }
    private static List<String> tokens(Pattern pattern, String value, boolean sorted) {
        var stream = pattern.matcher(value).results().map(java.util.regex.MatchResult::group);
        return (sorted ? stream.sorted() : stream).toList();
    }
    /** A script sanity check, not a claim of semantic or native-speaker quality verification. */
    static void validateLanguage(TranslatedContent translated, String locale) {
        String text = TAG.matcher(translated.title() + " " + translated.summary() + " " + translated.description()).replaceAll("");
        text = PROTECTED.matcher(text).replaceAll("");
        String script = switch (locale) {
            case "en" -> "\\p{IsLatin}";
            case "ja" -> "[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]";
            case "zh-CN" -> "\\p{IsHan}";
            default -> "\\p{IsHangul}";
        };
        if (!Pattern.compile(script).matcher(text).find()) throw new TranslationFailure(INVALID_RESPONSE);
    }
}
