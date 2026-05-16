package com.onec.fixtures;

import com.onec.annotations.Dimension;
import com.onec.annotations.InformationRegister;
import com.onec.annotations.Resource;
import com.onec.model.InformationRecord;
import com.onec.model.Periodicity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@InformationRegister(name = "Prices", periodicity = Periodicity.DAY)
public class TestPriceRegister extends InformationRecord {

    @Dimension
    private UUID product;

    @Dimension
    private UUID warehouse;

    @Resource(precision = 15, scale = 2)
    private BigDecimal price;
}
