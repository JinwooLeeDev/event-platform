package com.busan.eventplatform.event.translation;

import java.time.Duration;

/** Only safe codes may be persisted/logged. Never retain an HTTP body or credentials. */
public final class TranslationFailure extends RuntimeException {
    public enum Code {
        KEY_MISSING, MODEL_INVALID, REQUEST_INVALID, AUTHENTICATION, RATE_LIMIT, TIMEOUT, TEMPORARY,
        INVALID_RESPONSE, CONTENT_INVALID, SOURCE_CHANGED, NOT_PUBLIC, NOT_FOUND,
        SCHEMA_NOT_READY, LEASE_EXPIRED, INTERNAL
    }
    private final Code code;
    private final Duration retryAfter;
    public TranslationFailure(Code code) { this(code, Duration.ZERO); }
    public TranslationFailure(Code code, Duration retryAfter) {
        super(code.name()); this.code = code;
        this.retryAfter = retryAfter == null || retryAfter.isNegative() ? Duration.ZERO : retryAfter;
    }
    public Code code() { return code; }
    public Duration retryAfter() { return retryAfter; }
    public boolean retryable() { return code == Code.RATE_LIMIT || code == Code.TIMEOUT || code == Code.TEMPORARY; }
}
