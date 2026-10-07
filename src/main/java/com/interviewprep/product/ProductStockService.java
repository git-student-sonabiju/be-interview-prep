package com.interviewprep.product;

import com.interviewprep.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Long productId, int quantity) {
        if (productRepository.decrementStockIfAvailable(productId, quantity) == 0) {
            Integer available = productRepository.findStockById(productId);
            if (available == null) {
                throw new ResourceNotFoundException("Product", productId);
            }
            throw new InsufficientStockException(productId, quantity, available);
        }
        evictCachedProduct(productId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long productId, int quantity) {
        if (productRepository.incrementStock(productId, quantity) > 0) {
            evictCachedProduct(productId);
        }
    }

    private void evictCachedProduct(Long productId) {
        Cache cache = cacheManager.getCache(ProductService.PRODUCT_CACHE);
        if (cache != null) {
            cache.evict(productId);
        }
    }
}
