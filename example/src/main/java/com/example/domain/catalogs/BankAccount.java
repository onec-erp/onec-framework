package com.example.domain.catalogs;

import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

@Catalog(name = "Bank Accounts", codeLength = 12, codePrefix = "BA-", context = "Rentals")
@Getter
@Setter
public class BankAccount extends CatalogObject {

    @Attribute(displayName = "Nominee", length = 200, required = true)
    private String nominee;

    @Attribute(displayName = "IBAN", length = 34, required = true)
    private String iban;

    @Attribute(displayName = "BIC / SWIFT", length = 12)
    private String bic;

    @Attribute(length = 100)
    private String bankName;
}
