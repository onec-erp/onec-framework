package com.onec.guesty;

import com.onec.guesty.model.CalendarDay;
import com.onec.guesty.model.CreateReservationRequest;
import com.onec.guesty.model.Guest;
import com.onec.guesty.model.Listing;
import com.onec.guesty.model.Page;
import com.onec.guesty.model.Reservation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link RestClient}-backed {@link GuestyClient}. Each call attaches a bearer token from the
 * {@link GuestyTokenManager}, maps non-2xx responses to {@link GuestyApiException}, refreshes the
 * token once on a 401, and backs off on 429/5xx per {@link GuestyProperties.Retry}.
 */
public class DefaultGuestyClient implements GuestyClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultGuestyClient.class);

    private final RestClient restClient;
    private final GuestyTokenManager tokenManager;
    private final GuestyProperties properties;

    public DefaultGuestyClient(RestClient.Builder builder, GuestyTokenManager tokenManager,
                               GuestyProperties properties) {
        this.restClient = builder.clone().baseUrl(properties.resolveBaseUrl()).build();
        this.tokenManager = tokenManager;
        this.properties = properties;
    }

    // --- Listings ---

    @Override
    public Page<Listing> listListings(Map<String, ?> query) {
        return get("/listings", query, new ParameterizedTypeReference<Page<Listing>>() {});
    }

    @Override
    public Listing getListing(String id, String fields) {
        return get("/listings/" + id, fields == null ? Map.of() : Map.of("fields", fields), Listing.class);
    }

    // --- Reservations ---

    @Override
    public Page<Reservation> listReservations(Map<String, ?> query) {
        return get("/reservations", query, new ParameterizedTypeReference<Page<Reservation>>() {});
    }

    @Override
    public Reservation getReservation(String id, String fields) {
        return get("/reservations/" + id, fields == null ? Map.of() : Map.of("fields", fields), Reservation.class);
    }

    @Override
    public Reservation createReservation(CreateReservationRequest request) {
        return post("/reservations", request, Reservation.class);
    }

    // --- Guests ---

    /** Guesty's {@code /guests-crud} requires a {@code columns} string; used when the caller gives none. */
    private static final String DEFAULT_GUEST_COLUMNS = "fullName firstName lastName email phone emails phones";

    @Override
    public Page<Guest> listGuests(Map<String, ?> query) {
        return get("/guests-crud", guestQuery(query), new ParameterizedTypeReference<Page<Guest>>() {});
    }

    /**
     * Normalises a guests query to what {@code /guests-crud} actually expects: a {@code columns} string
     * (required — its absence is a 400, not a 403). We accept the {@code fields} key the other endpoints
     * use and translate it, and default {@code columns} when neither is supplied.
     */
    private Map<String, Object> guestQuery(Map<String, ?> query) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        if (query != null) {
            out.putAll(query);
        }
        Object fields = out.remove("fields");
        if (!out.containsKey("columns")) {
            out.put("columns", fields != null ? fields.toString() : DEFAULT_GUEST_COLUMNS);
        }
        return out;
    }

    @Override
    public Guest getGuest(String id) {
        return get("/guests-crud/" + id, Map.of(), Guest.class);
    }

    // --- Calendar ---

    @Override
    public List<CalendarDay> getCalendar(String listingId, LocalDate from, LocalDate to) {
        CalendarResponse response = get(
                "/availability-pricing/api/calendar/listings/" + listingId,
                Map.of("startDate", from.toString(), "endDate", to.toString()),
                CalendarResponse.class);
        if (response == null || response.data() == null || response.data().days() == null) {
            return List.of();
        }
        return response.data().days();
    }

    // --- Escape hatches ---

    @Override
    public <T> T get(String path, Map<String, ?> query, Class<T> type) {
        return execute(() -> restClient.get()
                .uri(uri -> buildUri(uri, path, query))
                .header("Authorization", bearer())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type));
    }

    @Override
    public <T> T get(String path, Map<String, ?> query, ParameterizedTypeReference<T> type) {
        return execute(() -> restClient.get()
                .uri(uri -> buildUri(uri, path, query))
                .header("Authorization", bearer())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type));
    }

    @Override
    public <T> T post(String path, Object body, Class<T> type) {
        return execute(() -> restClient.post()
                .uri(uri -> buildUri(uri, path, Map.of()))
                .header("Authorization", bearer())
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::raise)
                .body(type));
    }

    // --- internals ---

    private String bearer() {
        return "Bearer " + tokenManager.currentToken();
    }

    private java.net.URI buildUri(UriBuilder uri, String path, Map<String, ?> query) {
        uri.path(path);
        if (query != null) {
            query.forEach((k, v) -> {
                if (v != null) {
                    uri.queryParam(k, v.toString());
                }
            });
        }
        return uri.build();
    }

    private void raise(org.springframework.http.HttpRequest req,
                       org.springframework.http.client.ClientHttpResponse res) throws IOException {
        throw new GuestyApiException(res.getStatusCode().value(), readBody(res.getBody()));
    }

    private String readBody(InputStream body) {
        try (InputStream in = body) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    /** Run {@code call}, refreshing the token once on a 401 and backing off on 429/5xx. */
    private <T> T execute(Supplier<T> call) {
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long backoff = properties.getRetry().getBackoffMs();
        boolean refreshed = false;
        for (int attempt = 1; ; attempt++) {
            try {
                return call.get();
            } catch (GuestyApiException ex) {
                if (ex.isUnauthorized() && !refreshed) {
                    log.debug("Guesty returned 401; refreshing token and retrying once");
                    tokenManager.invalidate();
                    refreshed = true;
                    continue; // token refresh doesn't consume a retry attempt
                }
                boolean transientErr = ex.isRateLimited() || ex.status() >= 500;
                if (transientErr && attempt < maxAttempts) {
                    long wait = backoff * (1L << (attempt - 1));
                    log.warn("Guesty call failed (HTTP {}); retrying in {}ms (attempt {}/{})",
                            ex.status(), wait, attempt, maxAttempts);
                    sleep(wait);
                    continue;
                }
                throw ex;
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new GuestyException("Interrupted while backing off before a Guesty retry", ie);
        }
    }

    /** Wrapper for the calendar endpoint's {@code { status, data: { days: [...] } }} envelope. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CalendarResponse(Data data) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Data(List<CalendarDay> days) {
        }
    }
}
