package com.s2886810.jewelagent.repository;


import com.s2886810.jewelagent.entity.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {
    Optional<DailyReport> findByReportDate(LocalDate date);
    List<DailyReport> findAllByOrderByReportDateDesc();
}
