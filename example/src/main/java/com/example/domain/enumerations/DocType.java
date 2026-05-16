package com.example.domain.enumerations;

import com.onec.annotations.Enumeration;

@Enumeration(name = "Document Types")
public enum DocType {
    PASSPORT,
    NATIONAL_ID,
    DRIVING_LICENSE,
    OTHER
}
