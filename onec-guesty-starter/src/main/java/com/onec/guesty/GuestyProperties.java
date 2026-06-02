package com.onec.guesty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "onec.guesty")
public class GuestyProperties {

    public static final String DEFAULT_BASE_URL = "https://open-api.guesty.com/v1";
    public static final String DEFAULT_AUTH_URL = "https://open-api.guesty.com/oauth2/token";

    private boolean enabled = false;

    /** Base URL for the Guesty Open API. */
    private String baseUrl = DEFAULT_BASE_URL;

    /** OAuth 2.0 token endpoint (client-credentials grant). */
    private String authUrl = DEFAULT_AUTH_URL;

    /** OAuth client id issued for the integration. */
    private String clientId;

    /** OAuth client secret issued for the integration. */
    private String clientSecret;

    /**
     * A pre-obtained access token. When set, the {@link GuestyTokenManager} serves this verbatim and
     * never calls the token endpoint — useful for environments that mint tokens out of band, or for a
     * cached token handed over manually. Subject to the same 24h validity as any Guesty token.
     */
    private String accessToken;

    /** Connect/read timeout for API calls, in milliseconds. */
    private int timeoutMs = 30_000;

    @NestedConfigurationProperty
    private Token token = new Token();

    @NestedConfigurationProperty
    private Retry retry = new Retry();

    public String resolveBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
    }

    public String resolveAuthUrl() {
        return (authUrl == null || authUrl.isBlank()) ? DEFAULT_AUTH_URL : authUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    /**
     * Token-cache policy. Guesty caps token requests at 5 per 24h, so the token MUST be reused.
     * The manager caches in memory and, when {@link #cacheFile} is set, persists the token across
     * restarts so a redeploy doesn't burn a fresh request.
     */
    public static class Token {
        /**
         * Refresh the token this many milliseconds before its stated expiry, to avoid racing a call
         * against an expiring token. Defaults to 5 minutes.
         */
        private long refreshSkewMs = 300_000;

        /**
         * Optional file the manager reads/writes the cached token to, so it survives restarts within
         * the 24h window. Defaults to {@code build/guesty/token.json}; set to empty to disable.
         */
        private String cacheFile = "build/guesty/token.json";

        public long getRefreshSkewMs() {
            return refreshSkewMs;
        }

        public void setRefreshSkewMs(long refreshSkewMs) {
            this.refreshSkewMs = refreshSkewMs;
        }

        public String getCacheFile() {
            return cacheFile;
        }

        public void setCacheFile(String cacheFile) {
            this.cacheFile = cacheFile;
        }
    }

    /** Retry policy for transient failures (HTTP 429 and 5xx). */
    public static class Retry {
        /** Max attempts for a single call, including the first. */
        private int maxAttempts = 3;

        /** Base backoff between attempts, in milliseconds; doubled each attempt. */
        private long backoffMs = 1_000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }
    }
}
