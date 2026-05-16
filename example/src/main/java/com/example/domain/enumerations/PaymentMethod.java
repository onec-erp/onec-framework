package com.example.domain.enumerations;

import com.onec.annotations.Enumeration;

@Enumeration(name = "Payment Methods")
public enum PaymentMethod {
    BANK_TRANSFER,
    CASH,
    CARD,
    BTC,
    OTHER
}
