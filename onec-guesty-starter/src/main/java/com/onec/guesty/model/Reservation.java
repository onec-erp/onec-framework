package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A Guesty reservation. {@link #listingId}/{@link #guestId} are the foreign keys; {@link #listing}
 * and {@link #guest} are lightweight embedded summaries Guesty includes inline. {@link #status} is a
 * free-form string (e.g. {@code inquiry}, {@code reserved}, {@code confirmed}, {@code canceled}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reservation(
        @JsonProperty("_id") String id,
        String confirmationCode,
        String accountId,
        String status,
        String source,
        String listingId,
        String guestId,
        Ref listing,
        GuestRef guest,
        ReservationMoney money,
        Integer nightsCount,
        @JsonProperty("checkIn") Instant checkIn,
        @JsonProperty("checkOut") Instant checkOut,
        @JsonProperty("createdAt") Instant createdAt) {

    /** Lightweight embedded listing summary ({@code _id} + {@code title}). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ref(@JsonProperty("_id") String id, String title) {
    }

    /** Lightweight embedded guest summary ({@code _id} + {@code fullName}). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GuestRef(@JsonProperty("_id") String id, String fullName) {
    }
}
