package com.interviewprep.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSeeder implements ApplicationRunner {

    static final int SEED_COUNT = 100;
    private static final List<String> CATEGORIES = List.of("Electronics", "Books", "Clothing", "Home", "Sports");
    private static final List<String> ADJECTIVES = List.of("Classic", "Smart", "Compact", "Premium", "Eco");
    private static final List<String> NOUNS = List.of("Lamp", "Backpack", "Speaker", "Notebook", "Jacket", "Bottle", "Watch", "Chair");

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        Random random = new Random(42);
        List<Product> products = IntStream.rangeClosed(1, SEED_COUNT)
                .mapToObj(i -> new Product(
                        ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " " + NOUNS.get(random.nextInt(NOUNS.size())) + " " + i,
                        CATEGORIES.get(random.nextInt(CATEGORIES.size())),
                        BigDecimal.valueOf(5 + random.nextDouble() * 495).setScale(2, RoundingMode.HALF_UP),
                        random.nextInt(4) == 0 ? 0 : random.nextInt(200),
                        Math.round(random.nextDouble() * 50) / 10.0))
                .toList();
        productRepository.saveAll(products);
        log.info("Seeded {} products", products.size());
    }
}
