package com.example.domain.documents;

import com.example.domain.catalogs.BankAccount;
import com.example.domain.catalogs.Client;
import com.example.domain.enumerations.PaymentMethod;
import com.example.domain.registers.BankBalanceRegister;
import com.example.domain.registers.ReceivablesRegister;
import com.onec.annotations.AccessControl;
import com.onec.annotations.Attribute;
import com.onec.annotations.BusinessRule;
import com.onec.annotations.DashboardWidget;
import com.onec.annotations.Document;
import com.onec.annotations.UiHint;
import com.onec.annotations.UiSection;
import com.onec.lifecycle.Postable;
import com.onec.model.DocumentObject;
import com.onec.posting.PostingContext;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Document(name = "Payments", numberPrefix = "PMT-", numberLength = 14, context = "Rentals")
@AccessControl(readRoles = {"ADMIN", "RENTALS", "FINANCE"}, writeRoles = {"ADMIN", "FINANCE"})
@UiSection(value = "Finance", order = 1)
@BusinessRule(name = "client-required", expression = "client != null")
@BusinessRule(name = "amount-positive", expression = "amount > 0")
@DashboardWidget(title = "Recent Payments", type = "list", order = 4, width = "1/2", maxItems = 8)
@Getter
@Setter
public class Payment extends DocumentObject implements Postable {

    @Attribute(required = true)
    @UiHint(order = 0)
    private Ref<Client> client;

    @Attribute
    @UiHint(order = 1)
    private Ref<BankAccount> account;

    @Attribute
    @UiHint(order = 2)
    private PaymentMethod method;

    @Attribute(displayName = "Bill ref")
    @UiHint(order = 3)
    private UUID billRef;

    @Attribute(precision = 14, scale = 2, required = true)
    @UiHint(order = 4)
    private BigDecimal amount;

    @Attribute(length = 500)
    @UiHint(order = 5)
    private String notes;

    @Override
    public void handlePosting(PostingContext context) {
        // Settle the receivable: payment reduces what the client owes.
        var receivables = context.movements(ReceivablesRegister.class);
        receivables.addExpense(r -> {
            r.setClient(client);
            r.setAmount(amount);
        });

        if (account != null) {
            var bank = context.movements(BankBalanceRegister.class);
            bank.addReceipt(r -> {
                r.setAccount(account);
                r.setAmount(amount);
            });
        }
    }
}
