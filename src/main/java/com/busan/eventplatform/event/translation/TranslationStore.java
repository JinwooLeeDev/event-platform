package com.busan.eventplatform.event.translation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.busan.eventplatform.event.translation.TranslationFailure.Code.*;
import static com.busan.eventplatform.event.translation.TranslationResult.Status.*;

/** Portable JDBC queue. All mutations use short independent transactions; no HTTP occurs here. */
@Repository
public class TranslationStore {
    private final NamedParameterJdbcTemplate db;
    private final TransactionTemplate tx;
    private final TranslationProperties config;
    private final Clock clock;
    public record Snapshot(long id, SourceContent content, boolean published) {}
    public record Claim(long id, long eventId, String locale, String hash, String token, int retries) {}
    public TranslationStore(NamedParameterJdbcTemplate db, PlatformTransactionManager manager,
                            TranslationProperties config, @Qualifier("translationClock") Clock clock) {
        this.db = db; this.config = config; this.clock = clock;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(10);
    }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private Map<String, Object> parameters(Object... pairs) {
        var values = new HashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) values.put((String)pairs[i], pairs[i + 1]);
        return values;
    }
    public Snapshot source(long eventId) { return source(eventId, false); }
    /** One cache SELECT per page; compare against the exact Event snapshot used to build its DTOs. */
    public Map<Long, TranslatedContent> findCachedTranslations(Map<Long, SourceContent> sources, String locale) {
        if (sources.isEmpty() || locale.equals("ko")) return Map.of();
        // PostgreSQL aborts a transaction after a SQL error. Isolate optional cache reads so that
        // catching a missing-table/DB error cannot poison the original event query transaction.
        return tx.execute(ignored -> {
            var rows = db.queryForList("""
                select event_id,translated_title,translated_summary,translated_description,source_hash
                from event_translation where event_id in (:ids) and locale=:locale and status='COMPLETED'
                """, Map.of("ids", sources.keySet(), "locale", locale));
            var result = new LinkedHashMap<Long, TranslatedContent>();
            for (var row : rows) {
                long id = ((Number)row.get("event_id")).longValue();
                if (sources.get(id).hash().equals(row.get("source_hash"))) result.put(id,
                        new TranslatedContent((String)row.get("translated_title"), (String)row.get("translated_summary"), (String)row.get("translated_description")));
            }
            return result;
        });
    }
    private Snapshot source(long eventId, boolean lock) {
        var rows = db.query("select id,title,summary,description,status from events where id=:id" + (lock ? " for update" : ""),
                Map.of("id", eventId), (row, index) -> {
                    try { return new Snapshot(row.getLong("id"),
                            new SourceContent("ko", row.getString("title"), row.getString("summary"), row.getString("description")),
                            "PUBLISHED".equals(row.getString("status"))); }
                    catch (IllegalArgumentException invalidLegacyContent) { throw new TranslationFailure(CONTENT_INVALID); }
                });
        if (rows.isEmpty()) throw new TranslationFailure(TranslationFailure.Code.NOT_FOUND);
        return rows.getFirst();
    }
    private static TranslatedContent original(Snapshot source) {
        return new TranslatedContent(source.content.title(), source.content.summary(), source.content.description());
    }
    private TranslationResult result(TranslationResult.Status status, Snapshot source, TranslationFailure.Code error) {
        return new TranslationResult(status, original(source), source.content.hash(), error);
    }
    public TranslationResult evaluate(long eventId, String locale, boolean request, boolean retry,
                                      TranslationFailure.Code unavailable) {
        return tx.execute(ignored -> {
            Snapshot source = source(eventId, true);
            if (locale.equals("ko")) return result(ORIGINAL, source, null);
            if (!source.published) return result(FAILED, source, NOT_PUBLIC);
            var key = parameters("event", eventId, "locale", locale, "hash", source.content.hash(), "now", now());
            var cached = db.queryForList("select * from event_translation where event_id=:event and locale=:locale", key);
            if (!cached.isEmpty() && key.get("hash").equals(cached.getFirst().get("source_hash"))) {
                var row = cached.getFirst();
                return new TranslationResult(COMPLETED, new TranslatedContent((String)row.get("translated_title"),
                        (String)row.get("translated_summary"), (String)row.get("translated_description")), source.content.hash(), null);
            }
            var jobs = db.queryForList("select * from translation_job where event_id=:event and target_locale=:locale and source_hash=:hash", key);
            if (request && unavailable != null) return result(FAILED, source, unavailable);
            if (request && source.content.length() > config.getMaxSourceChars()) return result(FAILED, source, CONTENT_INVALID);
            if (jobs.isEmpty()) {
                if (!request) return result(cached.isEmpty() ? TranslationResult.Status.NOT_FOUND : STALE, source, null);
                db.update("""
                    insert into translation_job(event_id,target_locale,source_hash,status,retry_count,next_retry_at,created_at,updated_at)
                    values(:event,:locale,:hash,'PENDING',0,:now,:now,:now)
                    """, key);
                return result(PENDING, source, null);
            }
            var job = jobs.getFirst();
            String status = (String)job.get("status");
            if (request && retry && status.equals("FAILED") && ((Number)job.get("retry_count")).intValue() < config.getMaxRetries()) {
                db.update("""
                    update translation_job set status='PENDING',retry_count=retry_count+1,next_retry_at=:now,
                    last_error=null,completed_at=null,updated_at=:now
                    where event_id=:event and target_locale=:locale and source_hash=:hash
                    """, key);
                return result(PENDING, source, null);
            }
            if (status.equals("COMPLETED")) {
                if (!request) return result(STALE, source, null);
                // A -> B -> A: the single-locale cache may hold B while A's job already succeeded.
                // Start a new cache-fill cycle using the same unique job, not a duplicate row.
                // Failed cycles never enter here; their bounded retry count is not reset.
                db.update("""
                    update translation_job set status='PENDING',retry_count=0,next_retry_at=:now,
                    last_error=null,lease_token=null,lease_until=null,started_at=null,completed_at=null,updated_at=:now
                    where event_id=:event and target_locale=:locale and source_hash=:hash
                    """, key);
                return result(PENDING, source, null);
            }
            var error = job.get("last_error");
            return result(TranslationResult.Status.valueOf(status), source, error == null ? null : TranslationFailure.Code.valueOf((String)error));
        });
    }
    public void invalidate(long eventId) {
        tx.executeWithoutResult(ignored -> {
            source(eventId, true);
            var p = parameters("event", eventId, "now", now());
            db.update("delete from event_translation where event_id=:event", p);
            db.update("""
                update translation_job set status='FAILED',last_error='SOURCE_CHANGED',lease_token=null,
                lease_until=null,next_retry_at=null,completed_at=:now,updated_at=:now where event_id=:event
                """, p);
        });
    }
    public Claim claim() {
        return tx.execute(ignored -> {
            LocalDateTime now = now();
            var p = parameters("now", now, "max", config.getMaxRetries(), "retryAt", now.plus(config.getBackoff()));
            // Serializes reservations across processes. Never held during an API call.
            var budget = db.queryForMap("select * from translation_budget where id=1 for update", Map.of());
            db.update("""
                update translation_job set status=case when retry_count<:max then 'PENDING' else 'FAILED' end,
                retry_count=case when retry_count<:max then retry_count+1 else retry_count end,
                next_retry_at=case when retry_count<:max then :retryAt else null end,
                last_error='LEASE_EXPIRED',lease_token=null,lease_until=null,completed_at=:now,updated_at=:now
                where status='PROCESSING' and lease_until<=:now
                """, p);
            LocalDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);
            // Deliberately app UTC budget, independent of Google's Pacific-time quota reset.
            LocalDateTime day = now.toLocalDate().atStartOfDay();
            int minuteCount = minute.equals(time(budget.get("minute_start"))) ? ((Number)budget.get("minute_count")).intValue() : 0;
            int dayCount = day.equals(time(budget.get("day_start"))) ? ((Number)budget.get("day_count")).intValue() : 0;
            LocalDateTime blocked = time(budget.get("blocked_until"));
            if (blocked != null && blocked.isAfter(now) || minuteCount >= config.getRequestsPerMinute() || dayCount >= config.getRequestsPerDay()) return null;
            Long active = db.queryForObject("select count(*) from translation_job where status='PROCESSING' and lease_until>:now", p, Long.class);
            if (active != null && active >= config.getMaxConcurrent()) return null;
            var rows = db.queryForList("""
                select id,event_id,target_locale,source_hash,retry_count from translation_job
                where status='PENDING' and next_retry_at<=:now order by next_retry_at,id fetch first 1 rows only
                """, p);
            if (rows.isEmpty()) return null;
            var job = rows.getFirst();
            String token = UUID.randomUUID().toString();
            p.putAll(parameters("id", job.get("id"), "token", token, "until", now.plus(config.getLease()),
                    "minute", minute, "minuteCount", minuteCount + 1, "day", day, "dayCount", dayCount + 1));
            int changed = db.update("""
                update translation_job set status='PROCESSING',lease_token=:token,lease_until=:until,
                started_at=:now,completed_at=null,updated_at=:now where id=:id and status='PENDING'
                """, p);
            if (changed != 1) return null;
            db.update("update translation_budget set minute_start=:minute,minute_count=:minuteCount,day_start=:day,day_count=:dayCount where id=1", p);
            return new Claim(((Number)job.get("id")).longValue(), ((Number)job.get("event_id")).longValue(),
                    (String)job.get("target_locale"), (String)job.get("source_hash"), token, ((Number)job.get("retry_count")).intValue());
        });
    }
    private static LocalDateTime time(Object value) {
        return value == null ? null : value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime)value;
    }
    public void complete(Claim claim, TranslatedContent translated) {
        tx.executeWithoutResult(ignored -> {
            Snapshot source;
            try { source = source(claim.eventId, true); }
            catch (TranslationFailure missing) { return; } // Event/FK cascade removed both records.
            var p = parameters("id", claim.id, "token", claim.token, "now", now(), "event", claim.eventId, "locale", claim.locale);
            var owned = db.queryForList("select id from translation_job where id=:id and status='PROCESSING' and lease_token=:token and lease_until>:now for update", p);
            if (owned.isEmpty()) return;
            if (!source.published || !source.content.hash().equals(claim.hash)) {
                terminal(p, source.published ? SOURCE_CHANGED : NOT_PUBLIC); return;
            }
            TranslationValidator.validate(source.content, translated, config.getMaxOutputChars());
            TranslationValidator.validateLanguage(translated, claim.locale);
            p.putAll(parameters("title", translated.title(), "summary", translated.summary(), "description", translated.description(), "hash", claim.hash));
            int updated = db.update("""
                update event_translation set translated_title=:title,translated_summary=:summary,
                translated_description=:description,source_hash=:hash,status='COMPLETED',translated_at=:now,updated_at=:now
                where event_id=:event and locale=:locale
                """, p);
            if (updated == 0) db.update("""
                insert into event_translation(event_id,locale,translated_title,translated_summary,translated_description,
                source_hash,status,translated_at,created_at,updated_at)
                values(:event,:locale,:title,:summary,:description,:hash,'COMPLETED',:now,:now,:now)
                """, p);
            db.update("update translation_job set status='COMPLETED',last_error=null,lease_token=null,lease_until=null,completed_at=:now,updated_at=:now where id=:id", p);
        });
    }
    private void terminal(Map<String, Object> p, TranslationFailure.Code code) {
        p.put("error", code.name());
        db.update("""
            update translation_job set status='FAILED',last_error=:error,next_retry_at=null,
            lease_token=null,lease_until=null,completed_at=:now,updated_at=:now where id=:id
            """, p);
    }
    public void fail(Claim claim, TranslationFailure failure) {
        tx.executeWithoutResult(ignored -> {
            var p = parameters("id", claim.id, "token", claim.token, "now", now(), "error", failure.code().name());
            // Same order as claim(): budget -> job, avoiding a lock inversion on 429.
            db.queryForMap("select id from translation_budget where id=1 for update", Map.of());
            var owned = db.queryForList("select id from translation_job where id=:id and status='PROCESSING' and lease_token=:token and lease_until>:now for update", p);
            if (owned.isEmpty()) return;
            if (failure.retryable() && claim.retries < config.getMaxRetries()) {
                Duration delay = config.getBackoff().multipliedBy(1L << Math.min(claim.retries, 10));
                if (failure.retryAfter().compareTo(delay) > 0) delay = failure.retryAfter();
                // A malicious Retry-After must not overflow timestamps. Large deferrals remain conservative.
                if (delay.compareTo(Duration.ofDays(30)) > 0) delay = Duration.ofDays(30);
                p.put("next", now().plus(delay));
                db.update("""
                    update translation_job set status='PENDING',retry_count=retry_count+1,next_retry_at=:next,
                    last_error=:error,lease_token=null,lease_until=null,updated_at=:now where id=:id
                    """, p);
            } else terminal(p, failure.code());
            if (failure.code() == RATE_LIMIT) {
                Duration delay = failure.retryAfter().compareTo(config.getBackoff()) > 0 ? failure.retryAfter() : config.getBackoff();
                if (delay.compareTo(Duration.ofDays(30)) > 0) delay = Duration.ofDays(30);
                p.put("until", now().plus(delay));
                db.update("update translation_budget set blocked_until=case when blocked_until is null or blocked_until<:until then :until else blocked_until end where id=1", p);
            }
        });
    }
}
