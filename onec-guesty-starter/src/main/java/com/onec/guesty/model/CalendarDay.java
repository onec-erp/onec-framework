package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * One day of a listing's availability/pricing calendar, from
 * {@code GET /availability-pricing/api/calendar/listings/{id}}. {@link #status} is typically
 * {@code available}, {@code booked}, {@code reserved} or {@code unavailable}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarDay(
        String date,
        String listingId,
        String status,
        BigDecimal price,
        String currency,
        Integer minNights,
        Boolean isBasePrice,
        Boolean cta,
        Boolean ctd) {
}
