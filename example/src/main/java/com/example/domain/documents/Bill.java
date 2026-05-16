package com.example.domain.documents;

import com.example.domain.catalogs.Client;
import com.example.domain.catalogs.Property;
import com.example.domain.registers.ReceivablesRegister;
import com.example.domain.registers.RevenueRegister;
import com.onec.annotations.AccessControl;
import com.onec.annotations.Attribute;
import com.onec.annotations.BusinessRule;
import com.onec.annotations.DashboardWidget;
import com.onec.annotations.Document;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.lifecycle.BeforeWriteHandler;
import com.onec.lifecycle.Postable;
import com.onec.model.DocumentObject;
import com.onec.posting.PostingContext;
import com.onec.print.PrintFormat;
import com.onec.print.PrintTemplate;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Spanish VAT invoice mirroring the spreadsheet's "Bills" sheet.
 * Net + IVA = Gross. IVA percent defaults from the {@code DefaultIvaPercent} constant.
 */
@Document(name = "Bills", numberPrefix = "BILL-", numberLength = 14, context = "Rentals")
@AccessControl(readRoles = {"ADMIN", "RENTALS", "FINANCE"}, writeRoles = {"ADMIN", "FINANCE"})
@UiSection(value = "Finance", order = 0)
@BusinessRule(name = "client-required", expression = "client != null")
@BusinessRule(name = "gross-positive", expression = "gross > 0")
@PrintTemplate(name = "bill", label = "Print Bill", format = PrintFormat.PDF)
@DashboardWidget(title = "Recent Bills", type = "list", order = 3, width = "1/2", maxItems = 8)
@Getter
@Setter
public class Bill extends DocumentObject implements BeforeWriteHandler, Postable {

    @Attribute(required = true)
    @UiHint(order = 0)
    private Ref<Client> client;

    @Attribute
    @UiHint(order = 1)
    private Ref<Property> property;

    /** Backing field for the linked Booking; stored as raw UUID to keep this document free of cyclic references. */
    @Attribute(displayName = "Booking ref")
    @UiHint(order = 2)
    private UUID bookingRef;

    @Attribute(displayName = "Net (excl. IVA)", precision = 14, scale = 2)
    @UiHint(order = 3)
    private BigDecimal net;

    @Attribute(displayName = "IVA %", precision = 5, scale = 2)
    @UiHint(order = 4)
    private BigDecimal ivaPercent;

    @Attribute(displayName = "IVA amount", precision = 14, scale = 2)
    @UiHint(order = 5, visibleInForm = false)
    private BigDecimal iva;

    @Attribute(displayName = "Total (incl. IVA)", precision = 14, scale = 2)
    @UiHint(order = 6, visibleInForm = false)
    private BigDecimal gross;

    @Attribute(length = 1000)
    @UiHint(order = 10)
    private String comments;

    @Override
    public void beforeWrite() {
        BigDecimal n = net != null ? net : BigDecimal.ZERO;
        BigDecimal pct = ivaPercent != null ? ivaPercent : BigDecimal.ZERO;
        BigDecimal ivaAmount = n.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.iva = ivaAmount;
        this.gross = n.add(ivaAmount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void handlePosting(PostingContext context) {
        var receivables = context.movements(ReceivablesRegister.class);
        receivables.addReceipt(r -> {
            r.setClient(client);
            r.setAmount(gross);
        });

        var revenue = context.movements(RevenueRegister.class);
        revenue.addReceipt(r -> {
            r.setProperty(property);
            r.setNetAmount(net);
            r.setIvaAmount(iva);
            r.setGrossAmount(gross);
        });
    }
}
