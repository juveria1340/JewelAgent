package com.s2886810.jewelagent.kafka;

import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SaleEvent is the message format sent through Kafka.
 * Think of it as an envelope — when a sale is recorded, we put
 * all the sale details in here and publish it to the Kafka topic.
 * The consumer on the other end receives this same object.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleEvent {
    private UUID           id;
    private String         invoiceNumber;
    private String         customerName;
    private String         itemName;
    private String         itemCategory;
    private BigDecimal     itemWeight;
    private BigDecimal     price;
    private String         paymentMethod;
    private OffsetDateTime soldAt;
}