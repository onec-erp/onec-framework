package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A Guesty rental listing (property). Mirrors the fields most integrations need from
 * {@code GET /listings}; unrequested/unknown fields are ignored, so callers can narrow the payload
 * with the {@code fields} parameter without breaking deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Listing(
        @JsonProperty("_id") String id,
        String title,
        String nickname,
        String propertyType,
        String roomType,
        Integer accommodates,
        Double bedrooms,
        Double bathrooms,
        Integer beds,
        Boolean active,
        Boolean isListed,
        String timezone,
        String accountId,
        ListingAddress address,
        ListingPrices prices,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("lastUpdatedAt") Instant lastUpdatedAt) {
}
