package com.onec.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * High-level mail facade. Three modes of use:
 * <ul>
 *     <li>{@link #send(MailMessage)} - synchronous dispatch via the active provider.</li>
 *     <li>{@link #queue(MailMessage)} - durable queue via the mail outbox; relayed asynchronously with retry.</li>
 *     <li>{@link #send(Object, String, String...)} - render a registered {@link MailTemplate}
 *         bound to {@code target.getClass()} and dispatch (queued or direct based on config).</li>
 * </ul>
 * Default routing of {@link #send(Object, String, String...)} is controlled by {@code onec.mail.use-outbox}.
 */
public class MailService {

    private final MailDispatcher dispatcher;
    private final MailTemplateRegistry templates;
    private final MailRenderer renderer;
    private final MailProperties properties;
    private final MailOutbox outbox;
    private final ObjectMapper objectMapper;

    public MailService(MailDispatcher dispatcher,
                       MailTemplateRegistry templates,
                       MailRenderer renderer,
                       MailProperties properties,
                       MailOutbox outbox,
                       ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.templates = templates;
        this.renderer = renderer;
        this.properties = properties;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    /** Synchronous dispatch via the configured provider. */
    public void send(MailMessage message) {
        dispatcher.dispatch(message);
    }

    /** Durable queue. Picked up by the relay and dispatched asynchronously with retry/backoff. */
    public UUID queue(MailMessage message) {
        if (outbox == null) {
            throw new IllegalStateException(
                    "MailOutbox is not available; either disable onec.mail.use-outbox or configure a DataSource");
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            return outbox.enqueue(payload, dispatcher.name());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MailMessage", e);
        }
    }

    /** Render a template registered against {@code target.getClass()} and dispatch. */
    public void send(Object target, String templateName, Map<String, Object> extras, String... recipients) {
        MailMessage message = build(target, templateName, extras, recipients);
        if (properties.isUseOutbox() && outbox != null) {
            queue(message);
        } else {
            send(message);
        }
    }

    public void send(Object target, String templateName, String... recipients) {
        send(target, templateName, Map.of(), recipients);
    }

    public UUID queue(Object target, String templateName, Map<String, Object> extras, String... recipients) {
        return queue(build(target, templateName, extras, recipients));
    }

    private MailMessage build(Object target, String templateName,
                              Map<String, Object> extras, String[] recipients) {
        MailTemplateDescriptor descriptor = templates.find(target.getClass(), templateName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No mail template '" + templateName + "' on " + target.getClass().getName()));
        MailRenderer.Rendered rendered = renderer.render(descriptor, target, extras);

        MailMessage.Builder b = MailMessage.builder()
                .from(properties.getDefaultFrom())
                .to(recipients)
                .subject(rendered.subject());
        if (descriptor.replyTo() != null && !descriptor.replyTo().isBlank()) {
            b.replyTo(descriptor.replyTo());
        }
        if (rendered.html()) {
            b.html(rendered.body());
        } else {
            b.text(rendered.body());
        }
        return b.build();
    }
}
