package com.example.domain.registers;

import com.example.domain.catalogs.Property;
import com.onec.annotations.AccumulationRegister;
import com.onec.annotations.Dimension;
import com.onec.annotations.Resource;
import com.onec.annotations.UiSection;
import com.onec.model.AccumulationRecord;
import com.onec.model.AccumulationType;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AccumulationRegister(name = "Revenue", type = AccumulationType.TURNOVER, context = "Rentals")
@UiSection(value = "Reports", order = 8)
@Getter
@Setter
public class RevenueRegister extends AccumulationRecord {

    @Dimension
    private Ref<Property> property;

    @Resource(precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Resource(precision = 14, scale = 2)
    private BigDecimal ivaAmount;

    @Resource(precision = 14, scale = 2)
    private BigDecimal grossAmount;
}
