package com.example.domain.enumerations;

import com.onec.annotations.Enumeration;

@Enumeration(name = "OrderStatuses")
public enum OrderStatus {
    NEW, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
