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

@Configuration
public class UiConfig implements OneCUiConfigurer {

    @Override
    public void configure(UiLayoutBuilder layout) {
        layout.section("Rentals")
                .order(0)
                .icon("home")
                .catalog(Property.class)
                .catalog(Client.class)
                .document(Booking.class);

        layout.section("Finance")
                .order(1)
                .icon("euro")
                .document(Bill.class)
                .document(Payment.class)
                .catalog(BankAccount.class)
                .register(ReceivablesRegister.class)
                .register(BankBalanceRegister.class);

        layout.section("People")
                .order(2)
                .icon("users")
                .catalog(Employee.class);

        layout.section("Reports")
                .order(3)
                .icon("bar-chart")
                .register(OccupancyRegister.class)
                .register(RevenueRegister.class);

        layout.section("Reference")
                .order(9)
                .icon("book")
                .catalog(Country.class);

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
    }
}
