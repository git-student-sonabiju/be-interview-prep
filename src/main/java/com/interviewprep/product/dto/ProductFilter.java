package com.interviewprep.product.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record ProductFilter(
        String category,

        @DecimalMin(value = "0", message = "minPrice must not be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0", message = "maxPrice must not be negative")
        BigDecimal maxPrice,

        Boolean inStock,

        String name) {

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
