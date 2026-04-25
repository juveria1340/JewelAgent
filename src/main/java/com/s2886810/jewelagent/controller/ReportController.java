package com.s2886810.jewelagent.controller;

import com.s2886810.jewelagent.entity.DailyReport;
import com.s2886810.jewelagent.service.ArchiveService;
import com.s2886810.jewelagent.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService  reportService;
    private final ArchiveService archiveService;

    @GetMapping
    public ResponseEntity<List<DailyReport>> getAll() {
        return ResponseEntity.ok(reportService.getAll());
    }

    // POST /api/reports/generate?date=2024-06-01
    @PostMapping("/generate")
    public ResponseEntity<DailyReport> generate(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.generate(date));
    }

    // POST /api/reports/archive
    @PostMapping("/archive")
    public ResponseEntity<String> archive() {
        archiveService.archive();
        return ResponseEntity.ok("Archive complete. Check ./exports/");
    }
}