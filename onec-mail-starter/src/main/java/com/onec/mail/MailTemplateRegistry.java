package com.onec.mail;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MailTemplateRegistry {

    private final Map<Class<?>, Map<String, MailTemplateDescriptor>> byTarget = new LinkedHashMap<>();

    public void register(MailTemplateDescriptor descriptor) {
        byTarget.computeIfAbsent(descriptor.target(), k -> new LinkedHashMap<>())
                .put(descriptor.name(), descriptor);
    }

    public Optional<MailTemplateDescriptor> find(Class<?> target, String name) {
        return Optional.ofNullable(byTarget.getOrDefault(target, Map.of()).get(name));
    }
}
