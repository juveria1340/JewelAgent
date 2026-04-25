package com.s2886810.jewelagent.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * SaleConsumer is the RECEIVER side of Kafka.
 * It runs in the background and receives every SaleEvent published
 * to the topic. Right now it logs the event — but this is where you
 * could add real-time reactions: dashboard updates, stock alerts, etc.
 */
@Slf4j
@Component
public class SaleConsumer {

    @KafkaListener(
            topics  = "${jewelagent.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(SaleEvent event) {
        log.info("Received from Kafka [invoice={}, item={}, category={}, weight={}g, price={}, payment={}]",
                event.getInvoiceNumber(),
                event.getItemName(),
                event.getItemCategory(),
                event.getItemWeight(),
                event.getPrice(),
                event.getPaymentMethod());
    }
}