package com.onec.mail;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Renders mail subject and body templates. Subject is processed inline as a tiny Thymeleaf template
 * so it can reference {@code doc} fields ("Booking #${doc.ref} confirmed").
 */
public class MailRenderer {

    private final ResourceLoader resourceLoader;
    private final TemplateEngine engine;
    private final Charset encoding;

    public MailRenderer(ResourceLoader resourceLoader, MailProperties properties) {
        this.resourceLoader = resourceLoader;
        this.encoding = Charset.forName(properties.getEncoding());

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    public Rendered render(MailTemplateDescriptor descriptor, Object target, Map<String, Object> extras) {
        Context ctx = new Context();
        ctx.setVariable("doc", target);
        ctx.setVariable("self", target);
        if (extras != null) {
            ctx.setVariable("extra", extras);
            extras.forEach(ctx::setVariable);
        }

        String subject = engine.process(descriptor.subject(), ctx);

        Resource resource = resourceLoader.getResource(descriptor.template());
        if (!resource.exists()) {
            throw new IllegalStateException("Mail template not found: " + descriptor.template());
        }
        String body;
        try {
            body = new String(resource.getInputStream().readAllBytes(), encoding);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read template " + descriptor.template(), e);
        }
        String rendered = engine.process(body, ctx);

        return new Rendered(subject, rendered, descriptor.html());
    }

    public record Rendered(String subject, String body, boolean html) {
    }
}
