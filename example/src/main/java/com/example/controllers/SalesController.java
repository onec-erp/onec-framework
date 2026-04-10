package com.example.controllers;

import com.example.domain.registers.SalesRegister;
import com.example.repositories.SalesRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesRepository salesRepository;

    public SalesController(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    @GetMapping("/balance")
    public List<SalesRegister> getBalance(
            @RequestParam(required = false) String productName) {
        if (productName != null) {
            return salesRepository.getBalance(f -> f
                    .where(SalesRegister::getProductName, productName));
        }
        return salesRepository.getBalance();
    }
}
