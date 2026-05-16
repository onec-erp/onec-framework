package com.onec.mail;

import org.jdbi.v3.core.Jdbi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Side-effect queue for outbound mail. Lives in its own table {@code onec_mail_outbox}
 * rather than the framework's domain-event outbox: mail is a command-to-execute, not an event-to-broadcast,
 * and giving it a separate table avoids racing with the Kafka relay over the shared {@code onec_outbox}.
 */
public class MailOutbox {

    private static final String DDL =
            "CREATE TABLE IF NOT EXISTS onec_mail_outbox (\n" +
                    "    _id UUID PRIMARY KEY,\n" +
                    "    _payload TEXT NOT NULL,\n" +
                    "    _provider VARCHAR(64),\n" +
                    "    _attempts INT NOT NULL DEFAULT 0,\n" +
                    "    _last_error TEXT,\n" +
                    "    _created_at TIMESTAMP NOT NULL,\n" +
                    "    _dispatched_at TIMESTAMP,\n" +
                    "    _next_attempt_at TIMESTAMP NOT NULL,\n" +
                    "    _status VARCHAR(32) NOT NULL\n" +
                    ")";

    private final Jdbi jdbi;

    public MailOutbox(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void initSchema() {
        jdbi.useHandle(h -> h.execute(DDL));
    }

    public UUID enqueue(String payload, String provider) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        jdbi.useHandle(h -> h.createUpdate(
                        "INSERT INTO onec_mail_outbox " +
                                "(_id, _payload, _provider, _attempts, _created_at, _next_attempt_at, _status) " +
                                "VALUES (:id, :payload, :provider, 0, :now, :now, 'NEW')")
                .bind("id", id)
                .bind("payload", payload)
                .bind("provider", provider)
                .bind("now", now)
                .execute());
        return id;
    }

    public List<Pending> findDueForDispatch(int limit) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT _id, _payload, _attempts FROM onec_mail_outbox " +
                                "WHERE _status = 'NEW' AND _next_attempt_at <= :now " +
                                "ORDER BY _created_at LIMIT :limit")
                .bind("now", LocalDateTime.now())
                .bind("limit", limit)
                .map((rs, ctx) -> new Pending(
                        (UUID) rs.getObject("_id"),
                        rs.getString("_payload"),
                        rs.getInt("_attempts")))
                .list());
    }

    public void markDispatched(UUID id) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE onec_mail_outbox SET _status = 'SENT', _dispatched_at = :now " +
                                "WHERE _id = :id")
                .bind("id", id)
                .bind("now", LocalDateTime.now())
                .execute());
    }

    public void recordFailure(UUID id, int attempts, String error, LocalDateTime nextAttempt, boolean exhausted) {
        jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE onec_mail_outbox SET _attempts = :attempts, _last_error = :err, " +
                                "_next_attempt_at = :next, _status = :status WHERE _id = :id")
                .bind("id", id)
                .bind("attempts", attempts)
                .bind("err", error == null ? "" : error)
                .bind("next", nextAttempt)
                .bind("status", exhausted ? "FAILED" : "NEW")
                .execute());
    }

    public record Pending(UUID id, String payload, int attempts) {
    }
}
