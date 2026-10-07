package com.interviewprep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.product.dto.ProductRequest;
import com.interviewprep.product.dto.ProductResponse;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductSeeder productSeeder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void loadFixtures() {
        productRepository.deleteAll();
        productRepository.save(new Product("Smart Lamp", "Home", new BigDecimal("40.00"), 5, 4.5));
        productRepository.save(new Product("Smart Speaker", "Electronics", new BigDecimal("120.00"), 0, 4.1));
        productRepository.save(new Product("Smart Watch", "Electronics", new BigDecimal("199.99"), 12, 3.9));
        productRepository.save(new Product("Classic Watch", "Electronics", new BigDecimal("80.00"), 3, 4.8));
        productRepository.save(new Product("Running Shoes", "Sports", new BigDecimal("95.00"), 7, 4.2));
    }

    @Test
    void seederCreatesOneHundredProductsWhenCatalogIsEmpty() {
        productRepository.deleteAll();

        productSeeder.run(null);

        assertThat(productRepository.count()).isEqualTo(ProductSeeder.SEED_COUNT);
    }

    @Test
    void allFiltersCombineInOneRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("category", "electronics")
                        .param("minPrice", "50")
                        .param("maxPrice", "150")
                        .param("inStock", "true")
                        .param("name", "watch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Classic Watch"));
    }

    @Test
    void listIsPagedAndSortedWithTotals() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "2").param("page", "0").param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].name", contains("Smart Watch", "Smart Speaker")))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void pageSizeIsCappedAtOneHundred() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void sortingByUnknownFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "colour"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
    }

    @Test
    void invertedPriceRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("minPrice must not be greater than maxPrice"));
    }

    @Test
    void repeatedLookupsHitTheDatabaseOnlyOnce() {
        Long id = anyProductId();
        Statistics statistics = hibernateStatistics();
        statistics.clear();

        productService.get(id);
        productService.get(id);
        productService.get(id);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void lookupAfterUpdateReturnsFreshData() {
        Long id = anyProductId();
        productService.get(id);

        productService.update(id, new ProductRequest("Renamed", "Home", new BigDecimal("1.00"), 9, 5.0));
        Statistics statistics = hibernateStatistics();
        statistics.clear();
        ProductResponse afterUpdate = productService.get(id);

        assertThat(afterUpdate.name()).isEqualTo("Renamed");
        assertThat(afterUpdate.stock()).isEqualTo(9);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void lookupAfterDeleteReturnsNotFound() {
        Long id = anyProductId();
        productService.get(id);

        productService.delete(id);

        assertThrows(ResourceNotFoundException.class, () -> productService.get(id));
    }

    private Long anyProductId() {
        return productRepository.findAll().getFirst().getId();
    }

    private Statistics hibernateStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
