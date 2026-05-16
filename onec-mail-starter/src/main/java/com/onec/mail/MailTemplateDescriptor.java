package com.onec.mail;

public record MailTemplateDescriptor(
        Class<?> target,
        String name,
        String subject,
        String template,
        boolean html,
        String replyTo
) {
}
