package com.busan.eventplatform.event.translation;

import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;

/** One call processes at most one DB reservation; safe for multiple independently running workers. */
@Component
public class TranslationWorker {
    private final TranslationStore store;
    private final TranslationClient client;
    public TranslationWorker(TranslationStore store, TranslationClient client) { this.store = store; this.client = client; }
    public boolean runOne() {
        if (client.unavailableReason() != null) return false;
        TranslationStore.Claim claim;
        try { claim = store.claim(); } catch (DataAccessException schemaOrDatabaseUnavailable) { return false; }
        if (claim == null) return false;
        try {
            var source = store.source(claim.eventId());
            if (!source.published()) throw new TranslationFailure(NOT_PUBLIC);
            if (!source.content().hash().equals(claim.hash())) throw new TranslationFailure(SOURCE_CHANGED);
            // Intentionally outside any transaction, including the scheduler thread.
            var translated = client.translate(source.content(), claim.locale());
            store.complete(claim, translated);
        } catch (TranslationFailure failure) { store.fail(claim, failure);
        } catch (RuntimeException unexpected) { store.fail(claim, new TranslationFailure(INTERNAL)); }
        return true;
    }
}
