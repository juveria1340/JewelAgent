package com.s2886810.jewelagent.controller;

import com.s2886810.jewelagent.agent.AgentResponse;
import com.s2886810.jewelagent.agent.AgentService;
import com.s2886810.jewelagent.entity.Sale;
import com.s2886810.jewelagent.repository.DailyReportRepository;
import com.s2886810.jewelagent.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService          agentService;
    private final SaleRepository        saleRepository;
    private final DailyReportRepository reportRepository;

    /**
     * POST /api/agent/advise
     * Returns JSON with report and toolCallLog.
     */
    @PostMapping("/advise")
    public ResponseEntity<AgentResponse> advise() {
        log.info("Agent advisory requested (JSON)");
        return ResponseEntity.ok(agentService.runAdvisor());
    }

    /**
     * POST /api/agent/advise/html
     * Runs the agent and returns a self-contained HTML page
     * with the advisory report + live charts side by side.
     */
    @PostMapping(value = "/advise/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> adviseHtml() {
        log.info("Agent advisory requested (HTML)");

        // 1. Run the agent
        AgentResponse response = agentService.runAdvisor();

        // 2. Query live data for charts
        OffsetDateTime weekFrom = LocalDate.now().minusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime now      = OffsetDateTime.now();
        OffsetDateTime todayFrom = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Sale> weekSales  = saleRepository.findBySoldAtBetween(weekFrom, now);
        List<Sale> todaySales = saleRepository.findBySoldAtBetween(todayFrom, now);

        // Category revenue (last 7 days)
        Map<String, BigDecimal> revenueByCategory = weekSales.stream()
                .collect(Collectors.groupingBy(
                        Sale::getItemCategory,
                        Collectors.reducing(BigDecimal.ZERO, Sale::getPrice, BigDecimal::add)
                )).entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new
                ));

        String barLabels = revenueByCategory.keySet().stream()
                .map(k -> "\"" + k + "\"").collect(Collectors.joining(","));
        String barValues = revenueByCategory.values().stream()
                .map(BigDecimal::toPlainString).collect(Collectors.joining(","));

        // Payment method split (today)
        long cash = todaySales.stream().filter(s -> "CASH".equals(s.getPaymentMethod())).count();
        long card = todaySales.stream().filter(s -> "CARD".equals(s.getPaymentMethod())).count();

        // Summary numbers
        BigDecimal weekRevenue = weekSales.stream()
                .map(Sale::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayRevenue = todaySales.stream()
                .map(Sale::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Format the report text into HTML paragraphs
        String reportText = response.isSuccess() ? response.getReport() : "Agent did not return a report.";
        String reportHtml = formatReportAsHtml(reportText);

        // 4. Format tool call log
        String toolLog = response.getToolCallLog() != null
                ? response.getToolCallLog().replace("\n", "<br>").replace("->", "<b>&rarr;</b>")
                : "";

        String generatedAt = OffsetDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) + " UTC";

        String html = buildHtml(
                reportHtml, toolLog, generatedAt,
                barLabels, barValues,
                cash, card,
                todaySales.size(), weekSales.size(),
                todayRevenue.toPlainString(),
                weekRevenue.toPlainString()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String formatReportAsHtml(String text) {
        StringBuilder sb = new StringBuilder();
        String[] paragraphs = text.split("\n\n");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isBlank()) continue;
            // Detect numbered section headings like "1. TODAY'S SALES..."
            if (trimmed.matches("^\\d+\\.\\s+[A-Z].*") || trimmed.matches("^[A-Z][A-Z\\s]+$")) {
                sb.append("<h3>").append(trimmed).append("</h3>\n");
            } else if (trimmed.startsWith("1.") || trimmed.startsWith("2.") ||
                    trimmed.startsWith("3.") || trimmed.startsWith("4.") ||
                    trimmed.startsWith("5.")) {
                sb.append("<p class=\"rec\">").append(trimmed.replace("\n", " ")).append("</p>\n");
            } else {
                sb.append("<p>").append(trimmed.replace("\n", " ")).append("</p>\n");
            }
        }
        return sb.toString();
    }

    private String buildHtml(String reportHtml, String toolLog, String generatedAt,
                             String barLabels, String barValues,
                             long cash, long card,
                             int todayCount, int weekCount,
                             String todayRevenue, String weekRevenue) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>JewelAgent Advisory Report</title>\n" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js\"></script>\n" +
                "<style>\n" +
                "  * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;\n" +
                "         background: #f0f2f5; color: #1a2e4a; }\n" +
                "  .header { background: #1a2e4a; color: #fff; padding: 24px 32px;\n" +
                "            display: flex; justify-content: space-between; align-items: center; }\n" +
                "  .header h1 { font-size: 20px; font-weight: 600; }\n" +
                "  .header .meta { font-size: 12px; color: #94a3b8; }\n" +
                "  .body { display: grid; grid-template-columns: 1fr 1fr; gap: 20px;\n" +
                "          padding: 24px; max-width: 1400px; margin: 0 auto; }\n" +
                "  .cards { grid-column: 1 / -1; display: grid;\n" +
                "           grid-template-columns: repeat(4, 1fr); gap: 16px; }\n" +
                "  .card { background: #fff; border-radius: 10px; padding: 20px;\n" +
                "          border: 1px solid #e5e7eb; }\n" +
                "  .card-label { font-size: 11px; color: #6b7280; text-transform: uppercase;\n" +
                "                letter-spacing: 0.06em; margin-bottom: 8px; }\n" +
                "  .card-value { font-size: 26px; font-weight: 700; color: #1a2e4a; }\n" +
                "  .card-sub { font-size: 11px; color: #9ca3af; margin-top: 4px; }\n" +
                "  .panel { background: #fff; border-radius: 10px; padding: 24px;\n" +
                "           border: 1px solid #e5e7eb; }\n" +
                "  .panel-title { font-size: 14px; font-weight: 600; color: #1a2e4a;\n" +
                "                 margin-bottom: 16px; padding-bottom: 12px;\n" +
                "                 border-bottom: 1px solid #f3f4f6; }\n" +
                "  .report h3 { font-size: 13px; font-weight: 600; color: #2563a8;\n" +
                "               margin: 16px 0 8px; }\n" +
                "  .report p { font-size: 13px; line-height: 1.7; color: #374151;\n" +
                "              margin-bottom: 10px; }\n" +
                "  .report p.rec { background: #f0f9ff; border-left: 3px solid #2563a8;\n" +
                "                  padding: 8px 12px; border-radius: 0 6px 6px 0;\n" +
                "                  margin-bottom: 8px; }\n" +
                "  .charts-col { display: flex; flex-direction: column; gap: 20px; }\n" +
                "  .tool-log { grid-column: 1 / -1; }\n" +
                "  .tool-log-body { font-size: 12px; color: #374151; line-height: 1.8;\n" +
                "                   background: #f8fafc; padding: 16px; border-radius: 8px;\n" +
                "                   border: 1px solid #e5e7eb; }\n" +
                "  .footer { text-align: center; padding: 16px; font-size: 11px; color: #9ca3af; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"header\">\n" +
                "  <div>\n" +
                "    <h1>JewelAgent &mdash; Autonomous Inventory Advisory</h1>\n" +
                "    <div class=\"meta\">Student ID: s2886810</div>\n" +
                "  </div>\n" +
                "  <div class=\"meta\">Generated: " + generatedAt + "</div>\n" +
                "</div>\n" +
                "<div class=\"body\">\n" +
                "  <div class=\"cards\">\n" +
                "    <div class=\"card\">\n" +
                "      <div class=\"card-label\">Today's transactions</div>\n" +
                "      <div class=\"card-value\">" + todayCount + "</div>\n" +
                "      <div class=\"card-sub\">sales recorded today</div>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "      <div class=\"card-label\">Today's revenue</div>\n" +
                "      <div class=\"card-value\">&pound;" + todayRevenue + "</div>\n" +
                "      <div class=\"card-sub\">total sales value</div>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "      <div class=\"card-label\">Weekly transactions</div>\n" +
                "      <div class=\"card-value\">" + weekCount + "</div>\n" +
                "      <div class=\"card-sub\">last 7 days</div>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "      <div class=\"card-label\">Weekly revenue</div>\n" +
                "      <div class=\"card-value\">&pound;" + weekRevenue + "</div>\n" +
                "      <div class=\"card-sub\">last 7 days total</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"panel\">\n" +
                "    <div class=\"panel-title\">Advisory Report</div>\n" +
                "    <div class=\"report\">" + reportHtml + "</div>\n" +
                "  </div>\n" +
                "  <div class=\"charts-col\">\n" +
                "    <div class=\"panel\">\n" +
                "      <div class=\"panel-title\">Revenue by category (last 7 days)</div>\n" +
                "      <canvas id=\"barChart\"></canvas>\n" +
                "    </div>\n" +
                "    <div class=\"panel\">\n" +
                "      <div class=\"panel-title\">Payment methods (today)</div>\n" +
                "      <canvas id=\"donutChart\"></canvas>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"panel tool-log\">\n" +
                "    <div class=\"panel-title\">Agent tool call log</div>\n" +
                "    <div class=\"tool-log-body\">" + toolLog + "</div>\n" +
                "  </div>\n" +
                "</div>\n" +
                "<div class=\"footer\">JewelAgent &mdash; Spring Boot + Apache Kafka + PostgreSQL + Anthropic Claude</div>\n" +
                "<script>\n" +
                "new Chart(document.getElementById('barChart').getContext('2d'), {\n" +
                "  type: 'bar',\n" +
                "  data: {\n" +
                "    labels: [" + barLabels + "],\n" +
                "    datasets: [{\n" +
                "      label: 'Revenue',\n" +
                "      data: [" + barValues + "],\n" +
                "      backgroundColor: ['#2563a8','#1d9e75','#ba7517','#993c1d'],\n" +
                "      borderRadius: 6, borderSkipped: false\n" +
                "    }]\n" +
                "  },\n" +
                "  options: {\n" +
                "    responsive: true,\n" +
                "    plugins: { legend: { display: false },\n" +
                "      tooltip: { callbacks: { label: c => ' £' + c.parsed.y.toLocaleString('en-GB', {minimumFractionDigits:2}) } } },\n" +
                "    scales: { y: { beginAtZero: true,\n" +
                "      ticks: { callback: v => '£' + v.toLocaleString('en-GB') },\n" +
                "      grid: { color: '#f3f4f6' } }, x: { grid: { display: false } } }\n" +
                "  }\n" +
                "});\n" +
                "new Chart(document.getElementById('donutChart').getContext('2d'), {\n" +
                "  type: 'doughnut',\n" +
                "  data: {\n" +
                "    labels: ['Cash','Card'],\n" +
                "    datasets: [{ data: [" + cash + "," + card + "],\n" +
                "      backgroundColor: ['#1d9e75','#2563a8'], borderWidth: 0, hoverOffset: 6 }]\n" +
                "  },\n" +
                "  options: { responsive: true, cutout: '65%',\n" +
                "    plugins: { legend: { position: 'bottom' },\n" +
                "      tooltip: { callbacks: { label: c => ' ' + c.label + ': ' + c.parsed + ' sales' } } } }\n" +
                "});\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }
}