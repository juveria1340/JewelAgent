package com.s2886810.jewelagent.service;

import com.s2886810.jewelagent.entity.Sale;
import com.s2886810.jewelagent.kafka.SaleEvent;
import com.s2886810.jewelagent.kafka.SaleProducer;
import com.s2886810.jewelagent.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleProducer   saleProducer;

    @Transactional
    public Sale recordSale(Sale sale) {
        // 1. Save to Postgres
        Sale saved = saleRepository.save(sale);

        // 2. Publish to Kafka so the consumer receives it
        saleProducer.publish(SaleEvent.builder()
                .id(saved.getId())
                .invoiceNumber(saved.getInvoiceNumber())
                .customerName(saved.getCustomerName())
                .itemName(saved.getItemName())
                .itemCategory(saved.getItemCategory())
                .itemWeight(saved.getItemWeight())
                .price(saved.getPrice())
                .paymentMethod(saved.getPaymentMethod())
                .soldAt(saved.getSoldAt())
                .build());

        return saved;
    }

    public List<Sale> getAll() {
        return saleRepository.findAll();
    }
}