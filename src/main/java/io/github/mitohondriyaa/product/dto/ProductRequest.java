package io.github.mitohondriyaa.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Name mustn't be blank")
    String name,
    @NotBlank(message = "Description mustn't be blank")
    String description,
    @DecimalMin(value = "1.0", message = "Price must be at least 1.0")
    BigDecimal price
) {}