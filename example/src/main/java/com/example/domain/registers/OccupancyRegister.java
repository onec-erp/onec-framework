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

@AccumulationRegister(name = "Occupancy", type = AccumulationType.TURNOVER, context = "Rentals")
@UiSection(value = "Reports", order = 7)
@Getter
@Setter
public class OccupancyRegister extends AccumulationRecord {

    @Dimension
    private Ref<Property> property;

    @Resource(precision = 6, scale = 0)
    private BigDecimal nights;

    @Resource(precision = 6, scale = 0)
    private BigDecimal adults;

    @Resource(precision = 6, scale = 0)
    private BigDecimal children;
}
