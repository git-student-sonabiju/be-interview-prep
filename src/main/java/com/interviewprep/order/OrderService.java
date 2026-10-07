package com.interviewprep.order;

import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.order.dto.OrderItemRequest;
import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.order.dto.OrderResponse;
import com.interviewprep.product.Product;
import com.interviewprep.product.ProductRepository;
import com.interviewprep.product.ProductStockService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductStockService productStockService;
    private final TransactionTemplate transactionTemplate;

    public PlacedOrder place(String idempotencyKey, OrderRequest request) {
        SortedMap<Long, Integer> quantities = quantitiesByProduct(request);
        String fingerprint = fingerprint(quantities);
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> replay(existing, fingerprint))
                .orElseGet(() -> createOrReplay(idempotencyKey, fingerprint, quantities));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        if (orderRepository.markCancelledIfPlaced(id) == 0) {
            if (!orderRepository.existsById(id)) {
                throw new ResourceNotFoundException("Order", id);
            }
            throw new OrderNotCancellableException(id);
        }
        Order order = orderRepository.findWithItemsById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
        order.getItems().forEach(item -> productStockService.release(item.getProductId(), item.getQuantity()));
        return OrderResponse.from(order);
    }

    private PlacedOrder createOrReplay(String idempotencyKey, String fingerprint, SortedMap<Long, Integer> quantities) {
        try {
            OrderResponse created = transactionTemplate.execute(status -> create(idempotencyKey, fingerprint, quantities));
            return new PlacedOrder(created, true);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> replay(existing, fingerprint))
                    .orElseThrow(() -> concurrentDuplicate);
        }
    }

    private OrderResponse create(String idempotencyKey, String fingerprint, SortedMap<Long, Integer> quantities) {
        Map<Long, Product> products = productRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Order order = new Order(idempotencyKey, fingerprint);
        quantities.forEach((productId, quantity) -> {
            Product product = products.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product", productId);
            }
            order.addItem(new OrderItem(productId, quantity, product.getPrice()));
        });
        Order saved = orderRepository.saveAndFlush(order);
        quantities.forEach(productStockService::reserve);
        return OrderResponse.from(saved);
    }

    private PlacedOrder replay(Order existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return new PlacedOrder(OrderResponse.from(existing), false);
    }

    private SortedMap<Long, Integer> quantitiesByProduct(OrderRequest request) {
        return request.items().stream()
                .collect(Collectors.toMap(OrderItemRequest::productId, OrderItemRequest::quantity, Integer::sum, TreeMap::new));
    }

    private String fingerprint(SortedMap<Long, Integer> quantities) {
        String canonical = quantities.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record PlacedOrder(OrderResponse order, boolean created) {
    }
}
