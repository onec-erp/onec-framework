package com.example.domain.catalogs;

import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.annotations.UiSection;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Catalog(name = "Warehouses", codeLength = 5)
@UiSection(value = "Warehouse", order = 1)
@Getter
@Setter
@ToString(callSuper = true)
public class Warehouse extends CatalogObject {

    @Attribute(displayName = "Address", length = 300)
    private String address;

    @Attribute(displayName = "Contact Person", length = 100)
    private String contactPerson;
}
