package com.interviewprep.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewprep.order.dto.OrderItemRequest;
import com.interviewprep.order.dto.OrderRequest;
import com.interviewprep.product.Product;
import com.interviewprep.product.ProductRepository;
import com.interviewprep.product.ProductService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void fiftySimultaneousOrdersForStockOfTenLetExactlyTenSucceed() throws Exception {
        Long productId = productWithStock(10);
        OrderRequest request = orderOf(productId, 1);
        long ordersBefore = orderRepository.count();

        List<HttpStatusCode> statuses = runConcurrently(50, () -> placeOrder(UUID.randomUUID().toString(), request).getStatusCode());

        assertThat(statuses).filteredOn(status -> status.equals(HttpStatus.CREATED)).hasSize(10);
        assertThat(statuses).filteredOn(status -> status.equals(HttpStatus.CONFLICT)).hasSize(40);
        assertThat(stockOf(productId)).isZero();
        assertThat(orderRepository.count() - ordersBefore).isEqualTo(10);
    }

    @Test
    void retryingWithSameKeyReturnsTheOriginalOrderWithoutReservingTwice() {
        Long productId = productWithStock(5);
        String key = UUID.randomUUID().toString();
        OrderRequest request = orderOf(productId, 2);

        ResponseEntity<JsonNode> first = placeOrder(key, request);
        ResponseEntity<JsonNode> retry = placeOrder(key, request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getBody().get("id").asLong()).isEqualTo(first.getBody().get("id").asLong());
        assertThat(stockOf(productId)).isEqualTo(3);
        assertThat(orderRepository.findByIdempotencyKey(key)).isPresent();
    }

    @Test
    void simultaneousRetriesWithSameKeyCreateOnlyOneOrder() throws Exception {
        Long productId = productWithStock(100);
        String key = UUID.randomUUID().toString();
        OrderRequest request = orderOf(productId, 1);
        long ordersBefore = orderRepository.count();

        List<Long> orderIds = runConcurrently(10, () -> placeOrder(key, request).getBody().get("id").asLong());

        assertThat(orderIds).containsOnly(orderIds.getFirst());
        assertThat(orderRepository.count() - ordersBefore).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(99);
    }

    @Test
    void sameKeyWithDifferentItemsIsRejected() {
        Long productId = productWithStock(5);
        String key = UUID.randomUUID().toString();
        placeOrder(key, orderOf(productId, 1));

        ResponseEntity<JsonNode> response = placeOrder(key, orderOf(productId, 2));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(stockOf(productId)).isEqualTo(4);
    }

    @Test
    void insufficientStockOnAnyItemReservesNothing() {
        Long plentiful = productWithStock(5);
        Long scarce = productWithStock(1);
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(plentiful, 2), new OrderItemRequest(scarce, 2)));

        ResponseEntity<JsonNode> response = placeOrder(UUID.randomUUID().toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message").asText())
                .isEqualTo("Insufficient stock for product " + scarce + ": requested 2, available 1");
        assertThat(stockOf(plentiful)).isEqualTo(5);
        assertThat(stockOf(scarce)).isEqualTo(1);
    }

    @Test
    void cancellingReturnsStockAndCannotBeRepeated() {
        Long productId = productWithStock(5);
        long orderId = placeOrder(UUID.randomUUID().toString(), orderOf(productId, 3)).getBody().get("id").asLong();

        ResponseEntity<JsonNode> cancelled = restTemplate.postForEntity("/api/orders/{id}/cancel", null, JsonNode.class, orderId);
        ResponseEntity<JsonNode> cancelledAgain = restTemplate.postForEntity("/api/orders/{id}/cancel", null, JsonNode.class, orderId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelledAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stockOf(productId)).isEqualTo(5);
    }

    @Test
    void placingAnOrderRefreshesTheCachedProduct() {
        Long productId = productWithStock(5);
        productService.get(productId);

        placeOrder(UUID.randomUUID().toString(), orderOf(productId, 2));

        assertThat(productService.get(productId).stock()).isEqualTo(3);
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        Long productId = productWithStock(5);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/orders", orderOf(productId, 1), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(stockOf(productId)).isEqualTo(5);
    }

    private ResponseEntity<JsonNode> placeOrder(String idempotencyKey, OrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(OrderController.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        return restTemplate.postForEntity("/api/orders", new HttpEntity<>(request, headers), JsonNode.class);
    }

    private <T> List<T> runConcurrently(int callers, Callable<T> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < callers; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return call.call();
            }));
        }
        start.countDown();
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        pool.shutdown();
        return results;
    }

    private Long productWithStock(int stock) {
        return productRepository.save(new Product("Limited item", "Test", new BigDecimal("10.00"), stock, 4.0)).getId();
    }

    private OrderRequest orderOf(Long productId, int quantity) {
        return new OrderRequest(List.of(new OrderItemRequest(productId, quantity)));
    }

    private int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
