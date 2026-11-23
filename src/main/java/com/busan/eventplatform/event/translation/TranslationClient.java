package com.busan.eventplatform.event.translation;

public interface TranslationClient {
    TranslatedContent translate(SourceContent source, String targetLocale);
    default TranslationFailure.Code unavailableReason() { return null; }
}
