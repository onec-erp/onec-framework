package com.example.domain.registers;

import com.example.domain.catalogs.Client;
import com.onec.annotations.AccessControl;
import com.onec.annotations.AccumulationRegister;
import com.onec.annotations.Dimension;
import com.onec.annotations.Resource;
import com.onec.model.AccumulationRecord;
import com.onec.model.AccumulationType;
import com.onec.types.Ref;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * What each client currently owes — a {@link AccumulationType#BALANCE} register, so the framework
 * keeps a running total per dimension rather than period activity. Two documents move it in
 * opposite directions: {@link com.example.domain.documents.Bill} posts {@code addReceipt} (debt up)
 * and {@link com.example.domain.documents.Payment} posts {@code addExpense} (debt down). The net is
 * the outstanding balance per {@code client}.
 */
@AccumulationRegister(name = "Receivables", type = AccumulationType.BALANCE, context = "Rentals")
@AccessControl(readRoles = {"FINANCE"})
@Getter
@Setter
public class ReceivablesRegister extends AccumulationRecord {

    @Dimension
    private Ref<Client> client;

    @Resource(precision = 14, scale = 2)
    private BigDecimal amount;
}
