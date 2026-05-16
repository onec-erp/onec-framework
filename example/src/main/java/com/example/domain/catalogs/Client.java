package com.example.domain.catalogs;

import com.example.domain.enumerations.DocType;
import com.example.domain.enumerations.Gender;
import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.model.CatalogObject;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Catalog(name = "Clients", codeLength = 12, codePrefix = "C-", context = "Rentals")
@UiSection(value = "Rentals", order = 1)
@Getter
@Setter
public class Client extends CatalogObject {

    @Attribute(length = 100)
    @UiHint(order = 0)
    private String firstName;

    @Attribute(length = 100)
    @UiHint(order = 1)
    private String lastName1;

    @Attribute(length = 100)
    @UiHint(order = 2)
    private String lastName2;

    @Attribute(displayName = "Gender")
    @UiHint(order = 3)
    private Gender gender;

    @Attribute
    @UiHint(order = 4)
    private LocalDate birthday;

    @Attribute(displayName = "Document type")
    @UiHint(order = 5)
    private DocType docType;

    @Attribute(displayName = "ID / Passport No.", length = 50)
    @UiHint(order = 6)
    private String docNumber;

    @Attribute(displayName = "Document issued on")
    @UiHint(order = 7)
    private LocalDate docIssuedOn;

    @Attribute
    @UiHint(order = 8)
    private Ref<Country> nationality;

    @Attribute(length = 255)
    @UiHint(order = 9)
    private String address;

    @Attribute(length = 100)
    @UiHint(order = 10)
    private String city;

    @Attribute(length = 20)
    @UiHint(order = 11)
    private String postCode;

    @Attribute
    @UiHint(order = 12)
    private Ref<Country> country;

    @Attribute(length = 200)
    @UiHint(order = 13)
    private String email;

    @Attribute(length = 50)
    @UiHint(order = 14)
    private String mobile;
}
