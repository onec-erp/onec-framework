package com.onec.guesty;

import com.onec.guesty.model.CalendarDay;
import com.onec.guesty.model.CreateReservationRequest;
import com.onec.guesty.model.Guest;
import com.onec.guesty.model.Listing;
import com.onec.guesty.model.Page;
import com.onec.guesty.model.Reservation;

import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Thin, typed client over the Guesty Open API. Each method maps to a single HTTP call; token
 * acquisition, 401-refresh-and-retry, and 429/5xx backoff are handled transparently. Higher-level
 * conveniences (auto-pagination, domain mapping) live in {@link GuestyService}.
 *
 * <p>List endpoints accept a {@code query} map of raw Guesty parameters — {@code limit}, {@code skip},
 * {@code fields} (space-separated), {@code sort}, and endpoint-specific filters — giving callers the
 * full surface of the API without a method per filter.
 */
public interface GuestyClient {

    // --- Listings ---

    /** A page of listings. Common params: {@code limit}, {@code skip}, {@code fields}, {@code q}. */
    Page<Listing> listListings(Map<String, ?> query);

    /** A single listing by id. {@code fields} narrows the payload when supplied. */
    Listing getListing(String id, String fields);

    default Listing getListing(String id) {
        return getListing(id, null);
    }

    // --- Reservations ---

    /**
     * A page of reservations. Common params: {@code limit}, {@code skip}, {@code fields},
     * {@code sort}, {@code listingId}, and {@code filters} (a Guesty JSON filter expression).
     */
    Page<Reservation> listReservations(Map<String, ?> query);

    Reservation getReservation(String id, String fields);

    default Reservation getReservation(String id) {
        return getReservation(id, null);
    }

    /** Create a reservation. Returns the created resource. */
    Reservation createReservation(CreateReservationRequest request);

    // --- Guests ---

    /**
     * A page of guest CRM records from {@code /guests-crud}. Unlike the other list endpoints, this one
     * requires a <strong>{@code columns}</strong> query param (a space-separated <em>string</em> of field
     * names) and rejects the request with HTTP 400 {@code "columns must be a string"} if it is missing —
     * so {@link DefaultGuestyClient} supplies a sensible default when the caller omits it, and also
     * accepts the {@code fields} key used by the other endpoints, translating it to {@code columns}.
     * It also names the page total {@code total} rather than {@code count} (see {@link Page}).
     */
    Page<Guest> listGuests(Map<String, ?> query);

    Guest getGuest(String id);

    // --- Calendar ---

    /** The availability/pricing calendar for a listing across {@code [from, to]} inclusive. */
    List<CalendarDay> getCalendar(String listingId, LocalDate from, LocalDate to);

    // --- Escape hatches for endpoints not modelled above ---

    /** Arbitrary GET returning a typed body. */
    <T> T get(String path, Map<String, ?> query, Class<T> type);

    /** Arbitrary GET returning a generic typed body (for {@code Page<X>} etc.). */
    <T> T get(String path, Map<String, ?> query, ParameterizedTypeReference<T> type);

    /** Arbitrary POST with a body, returning a typed result. */
    <T> T post(String path, Object body, Class<T> type);
}
