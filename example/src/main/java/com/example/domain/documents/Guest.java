package com.example.domain.documents;

import com.example.domain.catalogs.Client;
import com.onec.annotations.Attribute;
import com.onec.model.TabularSectionRow;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Guest extends TabularSectionRow {

    @Attribute
    private Ref<Client> client;

    @Attribute(displayName = "Main guest")
    private boolean mainGuest;

    @Attribute(displayName = "Is child")
    private boolean isChild;
}
