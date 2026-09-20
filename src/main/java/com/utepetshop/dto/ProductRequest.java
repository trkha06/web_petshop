package com.utepetshop.dto;

import com.utepetshop.entity.PetType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank String sku,
    @NotBlank String name,
    String description,
    @NotNull @DecimalMin("0") BigDecimal price,
    @NotNull Long categoryId,
    @NotNull Long brandId,
    @NotNull PetType petType,
    String imageUrl) {}
