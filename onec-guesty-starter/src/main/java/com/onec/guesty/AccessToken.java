package com.onec.guesty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** A cached bearer token and the instant it stops being valid. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessToken(String value, Instant expiresAt) {

    /** Whether the token is still valid {@code skewMs} before its stated expiry. */
    public boolean isFresh(Instant now, long skewMs) {
        return value != null && !value.isBlank()
                && expiresAt != null
                && now.plusMillis(skewMs).isBefore(expiresAt);
    }
}
