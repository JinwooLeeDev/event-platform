package com.busan.eventplatform.event.translation;

/** content is always safe to use as fallback: original unless COMPLETED. */
public record TranslationResult(Status status, TranslatedContent content, String sourceHash, TranslationFailure.Code error) {
    public enum Status { ORIGINAL, COMPLETED, PENDING, PROCESSING, FAILED, NOT_FOUND, STALE }
}
