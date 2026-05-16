package com.onec.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "onec.mail")
public class MailProperties {

    private boolean enabled = true;

    /** Selects which {@link MailDispatcher} bean is active by its {@code name()}. */
    private String provider = "smtp";

    /** Default From: address when a {@link MailMessage} doesn't set one. */
    private String defaultFrom;

    /** Packages scanned for {@link MailTemplate}. Defaults to the application's base packages. */
    private List<String> basePackages = new ArrayList<>();

    /** Outbox relay batch size. */
    private int relayBatchSize = 50;

    /** Whether {@code MailService.queue(...)} writes to the outbox (true) or dispatches synchronously (false). */
    private boolean useOutbox = true;

    private String encoding = "UTF-8";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDefaultFrom() { return defaultFrom; }
    public void setDefaultFrom(String defaultFrom) { this.defaultFrom = defaultFrom; }
    public List<String> getBasePackages() { return basePackages; }
    public void setBasePackages(List<String> basePackages) { this.basePackages = basePackages; }
    public int getRelayBatchSize() { return relayBatchSize; }
    public void setRelayBatchSize(int relayBatchSize) { this.relayBatchSize = relayBatchSize; }
    public boolean isUseOutbox() { return useOutbox; }
    public void setUseOutbox(boolean useOutbox) { this.useOutbox = useOutbox; }
    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
}
