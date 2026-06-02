package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body for {@code POST /reservations}. {@link #listingId}, {@link #checkInDateLocalized} and
 * {@link #checkOutDateLocalized} are required; dates are local {@code yyyy-MM-dd} strings. Supply
 * either an existing {@link #guestId} or an inline {@link #guest} to create one. {@link #status}
 * defaults to {@code inquiry} when null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateReservationRequest(
        String listingId,
        String checkInDateLocalized,
        String checkOutDateLocalized,
        String guestId,
        GuestDetails guest,
        String status,
        Integer guestsCount,
        String source,
        BigDecimalMoney money) {

    public static CreateReservationRequest of(String listingId, String checkIn, String checkOut, String guestId) {
        return new CreateReservationRequest(listingId, checkIn, checkOut, guestId, null, null, null, null, null);
    }

    /** Inline guest to create alongside the reservation when no {@code guestId} is supplied. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GuestDetails(String firstName, String lastName, String email, String phone) {
    }

    /** Optional manual money override (e.g. a negotiated accommodation fare). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BigDecimalMoney(java.math.BigDecimal fareAccommodation, java.math.BigDecimal fareCleaning,
                                  String currency) {
    }
}
