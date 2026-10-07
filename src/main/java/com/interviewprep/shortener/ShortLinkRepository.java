package com.interviewprep.shortener;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("update ShortLink l set l.visitCount = l.visitCount + 1 where l.id = :id")
    void incrementVisitCount(@Param("id") Long id);
}
