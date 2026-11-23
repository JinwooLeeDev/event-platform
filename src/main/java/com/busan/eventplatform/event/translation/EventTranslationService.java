package com.busan.eventplatform.event.translation;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;

/** Translation boundary. Call mutation methods AFTER the Event transaction commits. */
@Service
public class EventTranslationService {
    private final TranslationStore store;
    private final TranslationClient client;
    public EventTranslationService(TranslationStore store, TranslationClient client) { this.store = store; this.client = client; }
    public TranslationResult requestTranslation(long eventId, String locale) { return evaluate(eventId, locale, true, false); }
    public TranslationResult findTranslation(long eventId, String locale) { return evaluate(eventId, locale, false, false); }
    public TranslationResult getTranslationStatus(long eventId, String locale) { return findTranslation(eventId, locale); }
    public TranslationResult retryTranslation(long eventId, String locale) { return evaluate(eventId, locale, true, true); }
    public boolean hasSourceChanged(long eventId, String hash) { return !store.source(eventId).content().hash().equals(hash); }
    public void invalidateTranslations(long eventId) { store.invalidate(eventId); }
    public java.util.Map<Long, TranslatedContent> findCachedTranslations(java.util.Map<Long, SourceContent> sources, String locale) {
        TranslationLocale.require(locale);
        return store.findCachedTranslations(sources, locale);
    }
    private TranslationResult evaluate(long eventId, String locale, boolean request, boolean retry) {
        TranslationLocale.require(locale);
        try { return store.evaluate(eventId, locale, request, retry, client.unavailableReason()); }
        catch (TranslationFailure failure) { return new TranslationResult(TranslationResult.Status.FAILED, null, null, failure.code()); }
        catch (DataAccessException unavailable) {
            // Tables are opt-in. A missing migration must not break the original event feature.
            try {
                TranslationStore.Snapshot source = store.source(eventId);
                var content = source.content();
                return new TranslationResult(TranslationResult.Status.FAILED,
                        new TranslatedContent(content.title(), content.summary(), content.description()), content.hash(),
                        client.unavailableReason() == null ? SCHEMA_NOT_READY : client.unavailableReason());
            } catch (TranslationFailure failure) {
                return new TranslationResult(TranslationResult.Status.FAILED, null, null, failure.code());
            } catch (DataAccessException databaseUnavailable) {
                return new TranslationResult(TranslationResult.Status.FAILED, null, null, SCHEMA_NOT_READY);
            }
        }
    }
}
