package com.onec.fixtures;

import com.onec.annotations.Enumeration;

@Enumeration(name = "OrderStatuses")
public enum TestOrderStatus {
    NEW, IN_PROGRESS, COMPLETED
}
