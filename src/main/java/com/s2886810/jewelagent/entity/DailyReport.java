package com.s2886810.jewelagent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "cash_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal cashRevenue;

    @Column(name = "card_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal cardRevenue;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @PrePersist @PreUpdate
    public void stamp() { generatedAt = OffsetDateTime.now(); }
}
