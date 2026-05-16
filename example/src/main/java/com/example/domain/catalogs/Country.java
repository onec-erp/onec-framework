package com.example.domain.catalogs;

import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

@Catalog(name = "Countries", codeLength = 3, context = "Rentals")
@UiSection(value = "Reference", order = 9)
@Getter
@Setter
public class Country extends CatalogObject {

    @Attribute(displayName = "ISO 2", length = 2)
    @UiHint(order = 0)
    private String iso2;

    @Attribute(displayName = "Name (English)", length = 100, required = true)
    @UiHint(order = 1)
    private String name;

    @Attribute(displayName = "Nationality", length = 100)
    @UiHint(order = 2)
    private String nationality;
}
