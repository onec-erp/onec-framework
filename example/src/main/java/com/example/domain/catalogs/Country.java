package com.example.domain.catalogs;

import com.onec.annotations.AccessControl;
import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

@Catalog(name = "Countries", codeLength = 3, context = "Rentals")
@AccessControl(readRoles = {"RENTALS", "FINANCE"}, writeRoles = {"RENTALS"})
@Getter
@Setter
public class Country extends CatalogObject {

    @Attribute(displayName = "ISO 2", length = 2)
    private String iso2;

    @Attribute(displayName = "Name (English)", length = 100, required = true)
    private String name;

    @Attribute(displayName = "Nationality", length = 100)
    private String nationality;
}
