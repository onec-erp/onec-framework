package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A guest CRM record. Reservations also carry a lightweight embedded form (id + {@code fullName})
 * under {@code reservation.guest}; the unset fields there are simply null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Guest(
        @JsonProperty("_id") String id,
        String fullName,
        String firstName,
        String lastName,
        List<String> emails,
        List<String> phones,
        String email,
        String phone) {
}
