package com.busan.eventplatform.event.translation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EventTranslationAfterCommit {
    private final EventTranslationService translations;
    public EventTranslationAfterCommit(EventTranslationService translations) { this.translations = translations; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaved(EventTranslationRequested request) { requestSafely(request.eventId(), request.locale()); }
    public void schedule(long eventId, String locale) {
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { requestSafely(eventId, locale); }
            });
        } else requestSafely(eventId, locale);
    }
    private void requestSafely(long eventId, String locale) {
        try { translations.requestTranslation(eventId, locale); }
        catch (RuntimeException ignored) {
            // Never let optional queue registration change a committed payment/publication or page result.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Event translation request unavailable for event {}", eventId);
        }
    }
}
