package com.busan.eventplatform.event.translation;

import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamReadFeature;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;

public final class GeminiTranslationClient implements TranslationClient {
    private final TranslationProperties config;
    private final HttpClient http;
    private final URI base;
    private final JsonMapper json = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final String instruction;
    public GeminiTranslationClient(TranslationProperties config, HttpClient http) {
        this(config, http, URI.create("https://generativelanguage.googleapis.com/v1beta/models/"));
    }
    // Local HTTP fixture injection is package-private; production endpoint is never configured by user content.
    GeminiTranslationClient(TranslationProperties config, HttpClient http, URI base) {
        this.config = config; this.http = http; this.base = base;
        try (var input = new ClassPathResource("translation/gemini-system.txt").getInputStream()) {
            instruction = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) { throw new IllegalStateException("Translation prompt unavailable"); }
    }
    @Override public TranslationFailure.Code unavailableReason() {
        if (config.getKey() == null || config.getKey().isBlank()) return KEY_MISSING;
        if (config.getModel() == null || !config.getModel().matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}")) return MODEL_INVALID;
        return null;
    }
    @Override public TranslatedContent translate(SourceContent source, String targetLocale) {
        TranslationLocale.require(targetLocale);
        if (unavailableReason() != null) throw new TranslationFailure(unavailableReason());
        if (source.length() > config.getMaxSourceChars()) throw new TranslationFailure(CONTENT_INVALID);
        try {
            var schema = Map.of("type", "object", "properties", Map.of("title", Map.of("type", "string"),
                    "summary", Map.of("type", "string"), "description", Map.of("type", "string")),
                    "required", List.of("title", "summary", "description"), "additionalProperties", false);
            String data = json.writeValueAsString(Map.of("sourceLocale", source.sourceLocale(), "targetLocale", targetLocale,
                    "title", source.title(), "summary", source.summary(), "description", source.description()));
            String body = json.writeValueAsString(Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", data)))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "responseJsonSchema", schema,
                            "temperature", 0.1, "maxOutputTokens", 16000)));
            var request = HttpRequest.newBuilder(URI.create(base.toString() + config.getModel() + ":generateContent"))
                    .timeout(config.getTimeout()).header("x-goog-api-key", config.getKey())
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            int limit = Math.max(65536, Math.min(config.getMaxOutputChars(), 200000) * 12);
            var pending = http.sendAsync(request, info -> info.statusCode() == 200
                    ? boundedBody(limit) : HttpResponse.BodySubscribers.replacing(new byte[0]));
            HttpResponse<byte[]> response;
            try { response = pending.get(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS); }
            catch (TimeoutException timeout) { pending.cancel(true); throw new TranslationFailure(TIMEOUT); }
            catch (InterruptedException interrupted) { pending.cancel(true); throw interrupted; }
            {
                int status = response.statusCode();
                if (status == 401 || status == 403) throw new TranslationFailure(AUTHENTICATION);
                if (status == 400) throw new TranslationFailure(REQUEST_INVALID);
                if (status == 404) throw new TranslationFailure(MODEL_INVALID);
                if (status == 429) throw new TranslationFailure(RATE_LIMIT, retryAfter(response.headers().firstValue("Retry-After").orElse("")));
                if (status == 408) throw new TranslationFailure(TIMEOUT);
                if (status >= 500) throw new TranslationFailure(TEMPORARY);
                if (status != 200) throw new TranslationFailure(INVALID_RESPONSE);
                // Bound untrusted body memory use; no response content is retained in exceptions/logs.
                byte[] bytes = response.body();
                var root = json.readTree(bytes);
                var candidates = root.path("candidates");
                if (!candidates.isArray() || candidates.size() != 1) throw new TranslationFailure(INVALID_RESPONSE);
                var candidate = candidates.get(0);
                if (!candidate.path("finishReason").asString().equals("STOP")) throw new TranslationFailure(INVALID_RESPONSE);
                var parts = candidate.path("content").path("parts");
                if (!parts.isArray()) throw new TranslationFailure(INVALID_RESPONSE);
                var text = new StringBuilder();
                for (var part : parts) {
                    if (part.path("thought").asBoolean(false)) continue;
                    if (!part.path("text").isString()) throw new TranslationFailure(INVALID_RESPONSE);
                    text.append(part.path("text").asString());
                }
                @SuppressWarnings("unchecked") Map<String, Object> fields = json.readValue(text.toString(), Map.class);
                if (!fields.keySet().equals(Set.of("title", "summary", "description"))
                        || fields.values().stream().anyMatch(value -> !(value instanceof String)))
                    throw new TranslationFailure(INVALID_RESPONSE);
                var result = new TranslatedContent((String) fields.get("title"), (String) fields.get("summary"), (String) fields.get("description"));
                TranslationValidator.validate(source, result, config.getMaxOutputChars());
                TranslationValidator.validateLanguage(result, targetLocale);
                return result;
            }
        } catch (TranslationFailure failure) { throw failure;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new TranslationFailure(TEMPORARY);
        } catch (ExecutionException transport) {
            for (Throwable cause = transport; cause != null; cause = cause.getCause()) {
                if (cause instanceof TranslationFailure failure) throw failure;
                if (cause instanceof HttpTimeoutException) throw new TranslationFailure(TIMEOUT);
            }
            throw new TranslationFailure(TEMPORARY);
        } catch (RuntimeException invalid) { throw new TranslationFailure(INVALID_RESPONSE); }
    }
    private static HttpResponse.BodySubscriber<byte[]> boundedBody(int limit) {
        return new HttpResponse.BodySubscriber<>() {
            final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
            Flow.Subscription subscription;
            int received;
            public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
            public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; delegate.onSubscribe(subscription); }
            public void onNext(List<ByteBuffer> buffers) {
                for (var buffer : buffers) {
                    received += buffer.remaining();
                    if (received > limit) { subscription.cancel(); delegate.onError(new TranslationFailure(INVALID_RESPONSE)); return; }
                }
                delegate.onNext(buffers);
            }
            public void onError(Throwable error) { delegate.onError(error); }
            public void onComplete() { delegate.onComplete(); }
        };
    }
    static Duration retryAfter(String value) {
        try { return Duration.ofSeconds(Math.max(0, Long.parseLong(value))); }
        catch (RuntimeException ignored) {
            try { return Duration.between(Instant.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).isNegative()
                    ? Duration.ZERO : Duration.between(Instant.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()); }
            catch (RuntimeException invalid) { return Duration.ZERO; }
        }
    }
}
