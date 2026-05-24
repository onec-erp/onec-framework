package com.example.domain.registers;

import com.example.domain.catalogs.BankAccount;
import com.onec.annotations.AccumulationRegister;
import com.onec.annotations.Dimension;
import com.onec.annotations.Resource;
import com.onec.model.AccumulationRecord;
import com.onec.model.AccumulationType;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AccumulationRegister(name = "Bank Balance", type = AccumulationType.BALANCE, context = "Rentals")
@Getter
@Setter
public class BankBalanceRegister extends AccumulationRecord {

    @Dimension
    private Ref<BankAccount> account;

    @Resource(precision = 14, scale = 2)
    private BigDecimal amount;
}
