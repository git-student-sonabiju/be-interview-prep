package com.interviewprep.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "items must contain at least one item")
        @Size(max = 50, message = "items must contain at most 50 items")
        List<@Valid OrderItemRequest> items) {
}
