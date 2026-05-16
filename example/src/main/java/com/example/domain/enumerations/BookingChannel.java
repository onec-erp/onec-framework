package com.example.domain.enumerations;

import com.onec.annotations.Enumeration;

@Enumeration(name = "Booking Channels")
public enum BookingChannel {
    DIRECT,
    AIRBNB,
    BOOKING_COM,
    VRBO,
    AGENCY,
    OTHER
}
