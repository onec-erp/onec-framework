package com.example.domain.enumerations;

import com.onec.annotations.Enumeration;

@Enumeration(name = "Booking Statuses")
public enum BookingStatus {
    DRAFT,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELED
}
