package com.interviewprep.product;

import com.interviewprep.common.PageResponse;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.product.dto.ProductFilter;
import com.interviewprep.product.dto.ProductRequest;
import com.interviewprep.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    public static final String PRODUCT_CACHE = "products";

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.matching(filter), pageable),
                ProductResponse::from);
    }

    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @CachePut(cacheNames = PRODUCT_CACHE, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setRating(request.rating());
        return ProductResponse.from(productRepository.saveAndFlush(product));
    }

    @CacheEvict(cacheNames = PRODUCT_CACHE, key = "#id")
    @Transactional
    public void delete(Long id) {
        productRepository.delete(findProduct(id));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
