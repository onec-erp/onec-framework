package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A listing's physical address, as returned under {@code listing.address}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingAddress(
        String full,
        String street,
        String city,
        String state,
        String zipcode,
        String country,
        Double lat,
        Double lng) {
}
