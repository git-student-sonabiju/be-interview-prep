package com.interviewprep.order;

import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.order.dto.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER)
            @NotBlank(message = "Idempotency-Key must not be blank")
            @Size(max = 100, message = "Idempotency-Key must be at most 100 characters")
            String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
        OrderService.PlacedOrder placed = orderService.place(idempotencyKey, request);
        HttpStatus status = placed.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(placed.order());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
