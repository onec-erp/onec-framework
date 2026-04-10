package com.example.domain.documents;

import com.example.domain.registers.SalesRegister;
import com.onec.annotations.Attribute;
import com.onec.annotations.DashboardWidget;
import com.onec.annotations.Document;
import com.onec.annotations.TabularSection;
import com.onec.annotations.UiHint;
import com.onec.lifecycle.BeforeWriteHandler;
import com.onec.lifecycle.Postable;
import com.onec.model.DocumentObject;
import com.onec.posting.PostingContext;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document(name = "Invoices", numberLength = 11)
@DashboardWidget(title = "Invoice Calendar", type = "calendar", order = 5, width = "1/3",
        dateField = "_date", titleField = "customer")
@Getter
@Setter
public class Invoice extends DocumentObject implements BeforeWriteHandler, Postable {

    @Attribute(length = 200)
    @UiHint(order = 0)
    private String customer;

    @Attribute(precision = 15, scale = 2)
    @UiHint(order = 1, visibleInForm = false)
    private BigDecimal total;

    @TabularSection(name = "items")
    private List<InvoiceLine> items = new ArrayList<>();

    @Override
    public void beforeWrite() {
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceLine line : items) {
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = line.getPrice() != null ? line.getPrice() : BigDecimal.ZERO;
            BigDecimal amount = qty.multiply(price);
            line.setAmount(amount);
            sum = sum.add(amount);
        }
        this.total = sum;
    }

    @Override
    public void handlePosting(PostingContext context) {
        var sales = context.movements(SalesRegister.class);
        for (InvoiceLine line : items) {
            sales.addReceipt(r -> {
                r.setProduct(line.getProduct());
                r.setQuantity(line.getQuantity());
                r.setAmount(line.getAmount());
            });
        }
    }
}
