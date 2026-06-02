package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * The financial summary of a reservation, as returned under {@code reservation.money}. Amounts are
 * in {@link #currency}. {@link #balanceDue} is what the guest still owes; {@link #hostPayout} is the
 * owner's expected take.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationMoney(
        String currency,
        BigDecimal fareAccommodation,
        BigDecimal fareCleaning,
        BigDecimal subTotalPrice,
        BigDecimal hostPayout,
        BigDecimal netIncome,
        BigDecimal totalPaid,
        BigDecimal balanceDue) {
}
