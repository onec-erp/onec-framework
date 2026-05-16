package com.example.domain.constants;

import com.onec.annotations.Constant;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Constant(name = "DefaultIvaPercent")
@Getter
@Setter
public class DefaultIvaPercent {
    private BigDecimal value;
}
