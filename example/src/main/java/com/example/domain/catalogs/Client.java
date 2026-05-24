package com.example.domain.catalogs;

import com.example.domain.enumerations.DocType;
import com.example.domain.enumerations.Gender;
import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.model.CatalogObject;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Catalog(name = "Clients", codeLength = 12, codePrefix = "C-", context = "Rentals")
@Getter
@Setter
public class Client extends CatalogObject {

    @Attribute(length = 100)
    private String firstName;

    @Attribute(length = 100)
    private String lastName1;

    @Attribute(length = 100)
    private String lastName2;

    @Attribute(displayName = "Gender")
    private Gender gender;

    @Attribute
    private LocalDate birthday;

    @Attribute(displayName = "Document type")
    private DocType docType;

    @Attribute(displayName = "ID / Passport No.", length = 50)
    private String docNumber;

    @Attribute(displayName = "Document issued on")
    private LocalDate docIssuedOn;

    @Attribute
    private Ref<Country> nationality;

    @Attribute(length = 255)
    private String address;

    @Attribute(length = 100)
    private String city;

    @Attribute(length = 20)
    private String postCode;

    @Attribute
    private Ref<Country> country;

    @Attribute(length = 200)
    private String email;

    @Attribute(length = 50)
    private String mobile;
}
