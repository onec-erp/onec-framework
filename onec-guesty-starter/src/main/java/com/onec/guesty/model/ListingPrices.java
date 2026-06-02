package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** A listing's default pricing, as returned under {@code listing.prices}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingPrices(
        String currency,
        BigDecimal basePrice,
        BigDecimal weekendBasePrice,
        BigDecimal cleaningFee,
        BigDecimal securityDepositFee,
        BigDecimal extraPersonFee,
        Integer guestsIncludedInRegularFee) {
}
