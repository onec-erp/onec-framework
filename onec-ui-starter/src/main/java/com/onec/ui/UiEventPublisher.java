package com.onec.ui;

import jakarta.annotation.PreDestroy;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UiEventPublisher {

    /**
     * How often to write a keepalive comment to each open stream. The browser, and any
     * proxy in between, will silently drop a connection that sits idle for long enough;
     * a ping well under the usual idle thresholds keeps the long-lived stream healthy and
     * lets us prune emitters whose socket has already gone away.
     */
    private static final long KEEPALIVE_SECONDS = 20;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService keepalive =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "onec-ui-events-keepalive");
                t.setDaemon(true);
                return t;
            });

    public UiEventPublisher() {
        keepalive.scheduleWithFixedDelay(this::ping,
                KEEPALIVE_SECONDS, KEEPALIVE_SECONDS, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        send(emitter, "ready", Map.of("type", "ready", "timestamp", Instant.now().toString()));
        return emitter;
    }

    public void publish(String type, String entityType, String entityName, Object id) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("entityType", entityType);
        payload.put("entityName", entityName);
        payload.put("id", id == null ? null : id.toString());
        payload.put("timestamp", Instant.now().toString());

        for (SseEmitter emitter : emitters) {
            send(emitter, type, payload);
        }
    }

    /**
     * Write a comment line ({@code : keepalive}) to every open stream. SSE comments carry
     * no event and the client parser ignores them, so this never surfaces as a UI event —
     * it just keeps the socket warm. A write that throws means the peer is gone, so we drop
     * the emitter.
     */
    private void ping() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
                emitter.completeWithError(e);
            }
        }
    }

    private void send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }
    }

    @PreDestroy
    void shutdown() {
        keepalive.shutdownNow();
    }
}
