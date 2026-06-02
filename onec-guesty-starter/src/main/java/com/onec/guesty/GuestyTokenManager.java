package com.onec.guesty;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Obtains and caches the Guesty OAuth 2.0 access token (client-credentials grant).
 *
 * <p>Guesty caps token <em>requests</em> at 5 per rolling 24h while each token is valid for 24h, so
 * a token MUST be reused rather than fetched per call. This manager:
 * <ul>
 *   <li>caches the token in memory and serves it until it nears expiry;</li>
 *   <li>optionally persists it to {@code onec.guesty.token.cache-file} so a restart reuses the same
 *       token instead of burning one of the five daily requests;</li>
 *   <li>serves a manually-supplied {@code onec.guesty.access-token} verbatim, never calling the
 *       token endpoint, when one is configured.</li>
 * </ul>
 *
 * <p>{@link #invalidate()} drops the cached token so the next {@link #currentToken()} re-authenticates;
 * the client calls it once on a 401 before retrying.
 */
public class GuestyTokenManager {

    private static final Logger log = LoggerFactory.getLogger(GuestyTokenManager.class);

    private final GuestyProperties properties;
    private final RestClient authClient;
    private final ObjectMapper objectMapper;

    private final Object lock = new Object();
    private volatile AccessToken cached;

    public GuestyTokenManager(GuestyProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.authClient = builder.clone().baseUrl(properties.resolveAuthUrl()).build();
        this.cached = readFromCacheFile();
    }

    /** A valid bearer token value, fetching/refreshing if necessary. */
    public String currentToken() {
        String fixed = properties.getAccessToken();
        if (fixed != null && !fixed.isBlank()) {
            return fixed;
        }
        long skew = properties.getToken().getRefreshSkewMs();
        AccessToken local = cached;
        if (local != null && local.isFresh(Instant.now(), skew)) {
            return local.value();
        }
        synchronized (lock) {
            if (cached != null && cached.isFresh(Instant.now(), skew)) {
                return cached.value();
            }
            AccessToken refreshed = requestToken();
            cached = refreshed;
            writeToCacheFile(refreshed);
            return refreshed.value();
        }
    }

    /** Drop the cached token so the next {@link #currentToken()} re-authenticates. No effect on a fixed token. */
    public void invalidate() {
        synchronized (lock) {
            cached = null;
        }
    }

    private AccessToken requestToken() {
        if (properties.getClientId() == null || properties.getClientSecret() == null) {
            throw new GuestyException(
                    "Cannot obtain a Guesty token: set onec.guesty.client-id and onec.guesty.client-secret "
                            + "(or supply onec.guesty.access-token).");
        }
        log.info("Requesting a new Guesty access token (capped at 5 per 24h)");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "open-api");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        Map<?, ?> response;
        try {
            response = authClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException ex) {
            throw new GuestyException("Guesty token request failed", ex);
        }
        if (response == null || response.get("access_token") == null) {
            throw new GuestyException("Guesty token response had no access_token: " + response);
        }
        String value = String.valueOf(response.get("access_token"));
        long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 86_400L;
        AccessToken token = new AccessToken(value, Instant.now().plusSeconds(expiresIn));
        log.info("Obtained Guesty token, valid for {}s", expiresIn);
        return token;
    }

    private AccessToken readFromCacheFile() {
        Path path = cacheFilePath();
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            AccessToken token = objectMapper.readValue(Files.readString(path), AccessToken.class);
            if (token.isFresh(Instant.now(), properties.getToken().getRefreshSkewMs())) {
                log.debug("Reusing cached Guesty token from {}", path);
                return token;
            }
        } catch (IOException | RuntimeException ex) {
            log.debug("Ignoring unreadable Guesty token cache at {}: {}", path, ex.getMessage());
        }
        return null;
    }

    private void writeToCacheFile(AccessToken token) {
        Path path = cacheFilePath();
        if (path == null) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, objectMapper.writeValueAsString(token));
        } catch (IOException ex) {
            log.warn("Could not persist Guesty token cache to {}: {}", path, ex.getMessage());
        }
    }

    private Path cacheFilePath() {
        String file = properties.getToken().getCacheFile();
        return (file == null || file.isBlank()) ? null : Path.of(file);
    }
}
