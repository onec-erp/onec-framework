package com.example.domain.documents;

import com.example.domain.catalogs.Client;
import com.onec.annotations.Attribute;
import com.onec.annotations.UiHint;
import com.onec.model.TabularSectionRow;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Guest extends TabularSectionRow {

    @Attribute
    @UiHint(order = 0)
    private Ref<Client> client;

    @Attribute(displayName = "Main guest")
    @UiHint(order = 1)
    private boolean mainGuest;

    @Attribute(displayName = "Is child")
    @UiHint(order = 2)
    private boolean isChild;
}
