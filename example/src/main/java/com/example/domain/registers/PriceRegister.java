package com.example.domain.registers;

import com.example.domain.catalogs.Product;
import com.onec.annotations.Dimension;
import com.onec.annotations.InformationRegister;
import com.onec.annotations.Resource;
import com.onec.model.InformationRecord;
import com.onec.model.Periodicity;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@InformationRegister(name = "Prices", periodicity = Periodicity.DAY)
@Getter
@Setter
public class PriceRegister extends InformationRecord {

    @Dimension(name = "product")
    private Ref<Product> product;

    @Resource(precision = 15, scale = 2)
    private BigDecimal price;
}
