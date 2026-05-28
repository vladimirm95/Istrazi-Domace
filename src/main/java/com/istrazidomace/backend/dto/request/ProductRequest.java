package com.istrazidomace.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Naziv je obavezan")
    @Size(max = 100, message = "Naziv može imati najviše 100 karaktera")
    private String title;

    @Size(max = 5000, message = "Opis može imati najviše 5000 karaktera")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Cena mora biti veća od 0")
    private BigDecimal price;

    @Size(max = 50, message = "Jedinica mere može imati najviše 50 karaktera")
    private String unit;
}