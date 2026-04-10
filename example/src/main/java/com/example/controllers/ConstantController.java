package com.example.controllers;

import com.example.domain.constants.CompanyName;
import com.onec.repository.ConstantManager;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/constants")
public class ConstantController {

    private final ConstantManager constantManager;

    public ConstantController(ConstantManager constantManager) {
        this.constantManager = constantManager;
    }

    @GetMapping("/company-name")
    public Map<String, String> getCompanyName() {
        String value = constantManager.get(CompanyName.class);
        return Map.of("value", value != null ? value : "");
    }

    @PutMapping("/company-name")
    public void setCompanyName(@RequestBody Map<String, String> body) {
        constantManager.set(CompanyName.class, body.get("value"));
    }
}
