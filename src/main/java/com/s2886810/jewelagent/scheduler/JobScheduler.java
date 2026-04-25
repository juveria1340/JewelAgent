package com.s2886810.jewelagent.scheduler;

import com.s2886810.jewelagent.service.ArchiveService;
import com.s2886810.jewelagent.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final ReportService  reportService;
    private final ArchiveService archiveService;

    // Runs at midnight every day — generates yesterday's report
//    @Scheduled(cron = "0 0 0 * * *")
    @Scheduled(cron = "0 */2 * * * *")
    public void dailyReport() {
        log.info("Scheduler: generating daily report");
        reportService.generate(LocalDate.now().minusDays(1));
    }

    // Runs at 01:00 AM every day — archives records older than 7 days
//    @Scheduled(cron = "0 0 1 * * *")
    @Scheduled(cron = "0 1/2 * * * *")
    public void archiveOldData() {
        log.info("Scheduler: running archive job");
        archiveService.archive();
    }
}