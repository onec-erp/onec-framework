package com.onec.guesty;

import com.onec.guesty.model.Listing;
import com.onec.guesty.model.Page;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live connectivity probe against the real Guesty Open API. Skipped unless credentials are supplied
 * via environment so no secret is ever committed:
 *
 * <pre>
 *   GUESTY_CLIENT_ID=... GUESTY_CLIENT_SECRET=... ./gradlew :onec-guesty-starter:test
 *   # or, to reuse an already-minted token (respects the 5-tokens/24h cap):
 *   GUESTY_ACCESS_TOKEN=eyJ... ./gradlew :onec-guesty-starter:test
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "GUESTY_CLIENT_ID", matches = ".+")
class GuestyLiveProbeTest {

    @Test
    void listsListingsAgainstLiveApi() {
        GuestyProperties props = new GuestyProperties();
        props.setEnabled(true);
        props.setClientId(System.getenv("GUESTY_CLIENT_ID"));
        props.setClientSecret(System.getenv("GUESTY_CLIENT_SECRET"));
        if (System.getenv("GUESTY_ACCESS_TOKEN") != null) {
            props.setAccessToken(System.getenv("GUESTY_ACCESS_TOKEN"));
        }
        // Cache the token under build/ so re-runs don't burn the 5/day request budget.
        props.getToken().setCacheFile("build/guesty/live-probe-token.json");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GuestyTokenManager tokens = new GuestyTokenManager(props, RestClient.builder(), mapper);
        GuestyClient client = new DefaultGuestyClient(RestClient.builder(), tokens, props);

        Page<Listing> page = client.listListings(Map.of("limit", 3));

        assertThat(page).isNotNull();
        assertThat(page.count()).isGreaterThanOrEqualTo(page.results().size());
        page.results().forEach(l -> {
            assertThat(l.id()).isNotBlank();
            System.out.printf("listing %s — %s (%s)%n", l.id(), l.title(),
                    l.prices() == null ? "?" : l.prices().currency());
        });
    }
}
