package com.s2886810.jewelagent.repository;

import com.s2886810.jewelagent.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findBySoldAtBetween(OffsetDateTime from, OffsetDateTime to);

    List<Sale> findBySoldAtBefore(OffsetDateTime cutoff);

    @Modifying
    @Query("DELETE FROM Sale s WHERE s.soldAt < :cutoff")
    int deleteBySoldAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
