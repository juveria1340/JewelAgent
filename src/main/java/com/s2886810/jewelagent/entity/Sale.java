package com.s2886810.jewelagent.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_category", nullable = true)
    private String itemCategory;

    @Column(name = "item_weight", nullable = false, precision = 8, scale = 3)
    private BigDecimal itemWeight;

    @Column(name = "price", nullable = false, precision = 4, scale = 2)
    private BigDecimal price;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;   // CASH or CARD

    @Column(name = "sold_at", nullable = false)
    private OffsetDateTime soldAt;

    @PrePersist
    public void prePersist() {
        if (soldAt == null) soldAt = OffsetDateTime.now();
    }
}
