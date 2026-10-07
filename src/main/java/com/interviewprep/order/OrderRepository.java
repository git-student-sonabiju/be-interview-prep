package com.interviewprep.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    @Modifying
    @Query("update Order o set o.status = com.interviewprep.order.OrderStatus.CANCELLED "
            + "where o.id = :id and o.status = com.interviewprep.order.OrderStatus.PLACED")
    int markCancelledIfPlaced(@Param("id") Long id);
}
