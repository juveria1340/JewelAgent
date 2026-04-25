package com.s2886810.jewelagent.service;

import com.s2886810.jewelagent.entity.DailyReport;
import com.s2886810.jewelagent.entity.Sale;
import com.s2886810.jewelagent.repository.DailyReportRepository;
import com.s2886810.jewelagent.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SaleRepository        saleRepository;
    private final DailyReportRepository reportRepository;

    @Transactional
    public DailyReport generate(LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to   = from.plusDays(1);

        List<Sale> sales = saleRepository.findBySoldAtBetween(from, to);

        DailyReport report = reportRepository.findByReportDate(date)
                .orElse(DailyReport.builder().reportDate(date).build());

        report.setTotalTransactions(sales.size());
        report.setTotalRevenue(sum(sales, null));
        report.setCashRevenue(sum(sales, "CASH"));
        report.setCardRevenue(sum(sales, "CARD"));

        DailyReport saved = reportRepository.save(report);
        log.info("Report generated [date={}, transactions={}, revenue={}]",
                date, sales.size(), saved.getTotalRevenue());
        return saved;
    }

    public List<DailyReport> getAll() {
        return reportRepository.findAllByOrderByReportDateDesc();
    }

    private BigDecimal sum(List<Sale> sales, String method) {
        return sales.stream()
                .filter(s -> method == null || method.equalsIgnoreCase(s.getPaymentMethod()))
                .map(Sale::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}