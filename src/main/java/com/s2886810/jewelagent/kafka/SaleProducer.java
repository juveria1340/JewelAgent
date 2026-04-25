package com.s2886810.jewelagent.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * SaleProducer is the SENDER side of Kafka.
 * Every time a sale is saved to the database, SaleService calls this class
 * to push a SaleEvent message onto the Kafka topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleProducer {

    private final KafkaTemplate<String, SaleEvent> kafkaTemplate;

    @Value("${jewelagent.kafka.topic}")
    private String topic;

    public void publish(SaleEvent event) {
        kafkaTemplate.send(topic, event.getInvoiceNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to Kafka [invoice={}]: {}",
                                event.getInvoiceNumber(), ex.getMessage());
                    } else {
                        log.info("Published to Kafka [invoice={}, item={}, price={}]",
                                event.getInvoiceNumber(), event.getItemName(), event.getPrice());
                    }
                });
    }
}