package com.example.domain.documents;

import com.example.domain.catalogs.Product;
import com.onec.annotations.Attribute;
import com.onec.annotations.UiHint;
import com.onec.model.TabularSectionRow;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GoodsReceiptLine extends TabularSectionRow {

    @Attribute
    private Ref<Product> product;

    @Attribute(precision = 15, scale = 2)
    @UiHint(order = 1)
    private BigDecimal quantity;

    @Attribute(precision = 15, scale = 2)
    @UiHint(order = 2)
    private BigDecimal unitCost;

    @Attribute(precision = 15, scale = 2)
    @UiHint(order = 3, visibleInForm = false)
    private BigDecimal totalCost;
}
