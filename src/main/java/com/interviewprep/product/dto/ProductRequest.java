package com.interviewprep.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "category is required")
        @Size(max = 255, message = "category must be at most 255 characters")
        String category,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0", message = "price must not be negative")
        BigDecimal price,

        @Min(value = 0, message = "stock must not be negative")
        int stock,

        @DecimalMin(value = "0", message = "rating must be between 0 and 5")
        @DecimalMax(value = "5", message = "rating must be between 0 and 5")
        double rating) {
}
