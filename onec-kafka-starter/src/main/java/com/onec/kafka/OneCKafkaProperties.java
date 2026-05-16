package com.onec.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "onec.kafka")
public class OneCKafkaProperties {

    private boolean enabled = true;
    private String serviceName = "onec-service";
    private String topic = "onec.domain-events";
    private int relayBatchSize = 100;
    private Map<String, String> remoteServices = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getRelayBatchSize() {
        return relayBatchSize;
    }

    public void setRelayBatchSize(int relayBatchSize) {
        this.relayBatchSize = relayBatchSize;
    }

    public Map<String, String> getRemoteServices() {
        return remoteServices;
    }

    public void setRemoteServices(Map<String, String> remoteServices) {
        this.remoteServices = remoteServices;
    }
}
