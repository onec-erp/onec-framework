package com.example.domain.catalogs;

import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

@Catalog(name = "Bank Accounts", codeLength = 12, codePrefix = "BA-", context = "Rentals")
@UiSection(value = "Finance", order = 5)
@Getter
@Setter
public class BankAccount extends CatalogObject {

    @Attribute(displayName = "Nominee", length = 200, required = true)
    @UiHint(order = 0)
    private String nominee;

    @Attribute(displayName = "IBAN", length = 34, required = true)
    @UiHint(order = 1)
    private String iban;

    @Attribute(displayName = "BIC / SWIFT", length = 12)
    @UiHint(order = 2)
    private String bic;

    @Attribute(length = 100)
    @UiHint(order = 3)
    private String bankName;
}
