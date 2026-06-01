package com.example;

import com.example.domain.catalogs.BankAccount;
import com.example.domain.catalogs.Client;
import com.example.domain.catalogs.Country;
import com.example.domain.catalogs.Employee;
import com.example.domain.catalogs.Property;
import com.example.domain.documents.Bill;
import com.example.domain.documents.Booking;
import com.example.domain.documents.Payment;
import com.example.domain.registers.BankBalanceRegister;
import com.example.domain.registers.OccupancyRegister;
import com.example.domain.registers.ReceivablesRegister;
import com.example.domain.registers.RevenueRegister;
import com.onec.ui.OneCUiConfigurer;
import com.onec.ui.UiLayoutBuilder;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of truth for UI placement and per-field hints.
 *
 * <p>Replaces {@code @UiSection}, {@code @UiHint}, and {@code @DashboardWidget}
 * on domain classes. Field hints set here override the deprecated annotations
 * via {@code UiLayoutResolver.resolveFieldHints}.</p>
 *
 * <p>Tabular section field hints (e.g. {@code Guest} inside {@code Booking})
 * are still configured via {@code @UiHint} on the row class; the DSL does not
 * yet support tabular section fields. See follow-up task #9.</p>
 */
@Configuration
public class UiConfig implements OneCUiConfigurer {

    @Override
    public void configure(UiLayoutBuilder layout) {
        layout.section("Rentals")
                .order(0)
                .icon("home")
                .catalog(Property.class, c -> c
                        .field("displayName").order(0)
                        .field("address").order(1)
                        .field("capacityAdults").order(2)
                        .field("defaultNightRate").order(3)
                        .field("cleaningFee").order(4))
                .catalog(Client.class, c -> c
                        .field("firstName").order(0)
                        .field("lastName1").order(1)
                        .field("lastName2").order(2)
                        .field("gender").order(3)
                        .field("birthday").order(4)
                        .field("docType").order(5)
                        .field("docNumber").order(6)
                        .field("docIssuedOn").order(7)
                        .field("nationality").order(8)
                        .field("address").order(9)
                        .field("city").order(10)
                        .field("postCode").order(11)
                        .field("country").order(12)
                        .field("email").order(13)
                        .field("mobile").order(14))
                .document(Booking.class, d -> d
                        .field("property").order(0)
                        .field("status").order(1)
                        .field("channel").order(2)
                        .field("checkIn").order(3)
                        .field("checkOut").order(4)
                        .field("adults").order(5)
                        .field("children").order(6)
                        .field("nights").order(7).hideInForm()
                        .field("nightRate").order(8)
                        .field("cleaningFee").order(9)
                        .field("totalGross").order(10).hideInForm()
                        .field("summary").order(11).hideInForm()
                        .field("primaryClient").order(12)
                        .field("assignedTo").order(13)
                        .field("notes").order(20));

        layout.section("Finance")
                .order(1)
                .icon("euro")
                .document(Bill.class, d -> d
                        .field("client").order(0)
                        .field("property").order(1)
                        .field("bookingRef").order(2)
                        .field("net").order(3)
                        .field("ivaPercent").order(4)
                        .field("iva").order(5).hideInForm()
                        .field("gross").order(6).hideInForm()
                        .field("comments").order(10))
                .document(Payment.class, d -> d
                        .field("client").order(0)
                        .field("account").order(1)
                        .field("method").order(2)
                        .field("billRef").order(3)
                        .field("amount").order(4)
                        .field("notes").order(5))
                .catalog(BankAccount.class, c -> c
                        .field("nominee").order(0)
                        .field("iban").order(1)
                        .field("bic").order(2)
                        .field("bankName").order(3))
                .register(ReceivablesRegister.class)
                .register(BankBalanceRegister.class);

        layout.section("People")
                .order(2)
                .icon("users")
                .catalog(Employee.class, c -> c
                        .field("avatarUrl").order(-1).widget("avatar")
                        .field("fullName").order(0)
                        .field("role").order(1)
                        .field("department").order(2)
                        .field("hourlyRate").order(3)
                        .field("hiredOn").order(4)
                        .field("email").order(5)
                        .field("mobile").order(6)
                        .field("active").order(7));

        layout.section("Reports")
                .order(3)
                .icon("bar-chart")
                .register(OccupancyRegister.class)
                .register(RevenueRegister.class);

        layout.section("Reference")
                .order(9)
                .icon("book")
                .catalog(Country.class, c -> c
                        .field("iso2").order(0)
                        .field("name").order(1)
                        .field("nationality").order(2));

        layout.widget("Properties")
                .type("count").order(0).width("1/4")
                .catalog(Property.class);

        layout.widget("Clients")
                .type("count").order(1).width("1/4")
                .catalog(Client.class);

        layout.widget("Upcoming bookings")
                .type("count").order(2).width("1/4")
                .document(Booking.class);

        layout.widget("Open bills")
                .type("count").order(3).width("1/4")
                .document(Bill.class);

        layout.widget("Bookings calendar")
                .type("calendar").order(4).width("full")
                .document(Booking.class)
                .dateField("check_in")
                .titleField("summary")
                .config("endDateField", "check_out")
                .config("secondaryField", "client_display,property_display");

        layout.widget("Bookings by status")
                .type("kanban").order(5).width("1/2")
                .document(Booking.class)
                .config("groupBy", "_posted")
                .maxItems(12);

        layout.widget("Revenue by property")
                .type("chart").order(6).width("1/2")
                .document(Bill.class)
                .config("kind", "bar")
                .config("groupBy", "property_display")
                .config("metric", "sum")
                .config("metricField", "total");

        layout.widget("Bills by status")
                .type("chart").order(7).width("1/2")
                .document(Bill.class)
                .config("kind", "donut")
                .config("groupBy", "_posted")
                .config("metric", "count");

        layout.widget("Recent bills")
                .type("list").order(8).width("1/2")
                .document(Bill.class)
                .maxItems(8);

        // Persona: cleaning staff get a focused, task-oriented app instead of the
        // full back-office layout. Curation only — RBAC still gates the data.
        var cleaning = layout.profile("cleaning")
                .roles("CLEANER")
                .title("Cleaning")
                .theme("teal")
                .priority(10);
        cleaning.section("Today")
                .order(0).icon("sparkles")
                .document(Booking.class, d -> d
                        .field("property").order(0)
                        .field("checkOut").order(1)
                        .field("status").order(2)
                        .field("assignedTo").order(3));
        cleaning.section("Team")
                .order(1).icon("users")
                .catalog(Employee.class);

        // Link login accounts to Employee records, matched on email, so persona
        // UIs can greet and (later) scope to the signed-in person.
        layout.identity(Employee.class, "email");
    }
}
