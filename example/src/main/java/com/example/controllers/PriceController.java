package com.example.controllers;

import com.example.domain.catalogs.Product;
import com.example.domain.registers.PriceRegister;
import com.onec.repository.InformationRegisterRepositoryImpl;
import com.onec.types.Ref;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final InformationRegisterRepositoryImpl<PriceRegister> priceRepository;

    @SuppressWarnings("unchecked")
    public PriceController(Map<Class<?>, InformationRegisterRepositoryImpl<?>> repoMap) {
        this.priceRepository = (InformationRegisterRepositoryImpl<PriceRegister>) repoMap.get(PriceRegister.class);
    }

    @PostMapping
    public void setPrice(@RequestParam UUID productId,
                         @RequestParam BigDecimal price) {
        PriceRegister record = new PriceRegister();
        record.setProduct(Ref.of(Product.class, productId));
        record.setPrice(price);
        record.setPeriod(LocalDateTime.now());
        priceRepository.write(record);
    }

    @GetMapping("/current")
    public List<PriceRegister> getCurrentPrices() {
        return priceRepository.getSliceLast(LocalDateTime.now());
    }

    @GetMapping("/at")
    public List<PriceRegister> getPricesAt(@RequestParam String date) {
        return priceRepository.getSliceLast(LocalDateTime.parse(date + "T23:59:59"));
    }

    @GetMapping
    public List<PriceRegister> getAllRecords() {
        return priceRepository.getRecords();
    }
}
