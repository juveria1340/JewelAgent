package com.s2886810.jewelagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.s2886810.jewelagent.entity.Sale;
import com.s2886810.jewelagent.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final SaleRepository saleRepository;
    private final ObjectMapper   objectMapper;

    @Value("${jewelagent.export.directory}")
    private String exportDir;

    @Value("${jewelagent.archive.retention-days}")
    private int retentionDays;

    @Transactional
    public void archive() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        List<Sale> old = saleRepository.findBySoldAtBefore(cutoff);

        if (old.isEmpty()) {
            log.info("Archive: nothing older than {} days to export", retentionDays);
            return;
        }

        String filename = String.format("archive_%s_%d_records.json", LocalDate.now(), old.size());
        try {
            Files.createDirectories(Paths.get(exportDir));
            File out = Paths.get(exportDir, filename).toFile();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, old);
            log.info("Archived {} records to {}", old.size(), out.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Archive export failed: " + e.getMessage(), e);
        }

        int deleted = saleRepository.deleteBySoldAtBefore(cutoff);
        log.info("Deleted {} records from sales table", deleted);
    }
}