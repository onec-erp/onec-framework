package com.example.domain.catalogs;

import com.onec.annotations.Attribute;
import com.onec.annotations.Catalog;
import com.onec.annotations.DashboardWidget;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.model.CatalogObject;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Catalog(name = "Employees", codeLength = 6, codePrefix = "E-", context = "Rentals")
@UiSection(value = "People", order = 3)
@DashboardWidget(title = "Employees", type = "count", order = 5, width = "1/4")
@Getter
@Setter
public class Employee extends CatalogObject {

    @Attribute(displayName = "Full name", length = 200, required = true)
    @UiHint(order = 0)
    private String fullName;

    @Attribute(displayName = "Avatar", length = 500)
    @UiHint(order = -1, widget = "avatar")
    private String avatarUrl;

    @Attribute(displayName = "Role", length = 50)
    @UiHint(order = 1)
    private String role;

    @Attribute(displayName = "Department", length = 50)
    @UiHint(order = 2)
    private String department;

    @Attribute(displayName = "Hourly rate", precision = 8, scale = 2)
    @UiHint(order = 3)
    private BigDecimal hourlyRate;

    @Attribute(displayName = "Hired on")
    @UiHint(order = 4)
    private LocalDate hiredOn;

    @Attribute(displayName = "Email", length = 200)
    @UiHint(order = 5)
    private String email;

    @Attribute(displayName = "Mobile", length = 50)
    @UiHint(order = 6)
    private String mobile;

    @Attribute(displayName = "Active")
    @UiHint(order = 7)
    private boolean active;
}
