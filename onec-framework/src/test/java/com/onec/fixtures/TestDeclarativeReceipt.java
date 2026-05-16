package com.onec.fixtures;

import com.onec.annotations.Attribute;
import com.onec.annotations.BusinessRule;
import com.onec.annotations.Document;
import com.onec.annotations.PostingRule;
import com.onec.annotations.TabularSection;
import com.onec.model.DocumentObject;
import com.onec.model.MovementType;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(name = "TestDeclarativeReceipts")
@BusinessRule(name = "warehouse-required", expression = "warehouse != null")
@BusinessRule(name = "items-required", expression = "items not empty")
@PostingRule(
        register = TestStockRegister.class,
        movement = MovementType.RECEIPT,
        forEach = "items",
        map = {
                "product = item.product",
                "warehouse = document.warehouse",
                "quantity = item.quantity"
        })
@Getter
@Setter
public class TestDeclarativeReceipt extends DocumentObject {

    @Attribute
    private UUID warehouse;

    @TabularSection(name = "items")
    private List<TestReceiptLine> items = new ArrayList<>();
}
