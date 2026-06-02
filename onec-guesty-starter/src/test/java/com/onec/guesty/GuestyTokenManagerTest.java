package com.onec.guesty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GuestyTokenManagerTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void servesFixedTokenWithoutNetwork() {
        GuestyProperties props = new GuestyProperties();
        props.setAccessToken("fixed-token-123");
        props.getToken().setCacheFile(""); // no persistence

        GuestyTokenManager manager = new GuestyTokenManager(props, RestClient.builder(), mapper);

        // No client id/secret and no auth server — proves it never calls the token endpoint.
        assertThat(manager.currentToken()).isEqualTo("fixed-token-123");
        manager.invalidate();
        assertThat(manager.currentToken()).isEqualTo("fixed-token-123");
    }

    @Test
    void reusesAFreshTokenFromCacheFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path cache = dir.resolve("token.json");
        AccessToken cached = new AccessToken("cached-token", Instant.now().plusSeconds(86_400));
        Files.writeString(cache, mapper.writeValueAsString(cached));

        GuestyProperties props = new GuestyProperties();
        props.getToken().setCacheFile(cache.toString());
        // No credentials: if it ignored the cache it would fail trying to authenticate.

        GuestyTokenManager manager = new GuestyTokenManager(props, RestClient.builder(), mapper);
        assertThat(manager.currentToken()).isEqualTo("cached-token");
    }

    @Test
    void failsClearlyWhenNoCredentialsAndNoToken() {
        GuestyProperties props = new GuestyProperties();
        props.getToken().setCacheFile("");

        GuestyTokenManager manager = new GuestyTokenManager(props, RestClient.builder(), mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(manager::currentToken)
                .isInstanceOf(GuestyException.class)
                .hasMessageContaining("client-id");
    }
}
