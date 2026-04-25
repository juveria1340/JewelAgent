package com.s2886810.jewelagent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.s2886810.jewelagent.entity.DailyReport;
import com.s2886810.jewelagent.entity.Sale;
import com.s2886810.jewelagent.repository.DailyReportRepository;
import com.s2886810.jewelagent.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AgentToolService defines what each tool actually does.
 *
 * The agent (Claude) decides which tools to call based on the question.
 * This class executes the tool, queries the database, and returns a
 * plain text result that gets sent back to the agent.
 *
 * Tools available:
 *   get_sales_today          — today's sales summary
 *   get_top_items            — best selling categories this week
 *   get_low_stock_categories — categories with low sales activity
 *   get_daily_report         — stored report for a specific date
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final SaleRepository        saleRepository;
    private final DailyReportRepository reportRepository;
    private final ObjectMapper          objectMapper;

    // ── Tool implementations ─────────────────────────────────────────────────

    public String getSalesToday() {
        OffsetDateTime from = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to   = from.plusDays(1);
        List<Sale> sales    = saleRepository.findBySoldAtBetween(from, to);

        if (sales.isEmpty()) return "No sales recorded today.";

        BigDecimal total = sales.stream()
                .map(Sale::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cash = sales.stream().filter(s -> "CASH".equals(s.getPaymentMethod())).count();
        long card = sales.stream().filter(s -> "CARD".equals(s.getPaymentMethod())).count();

        return String.format(
                "Today: %d sales | Total revenue: £%.2f | Cash payments: %d | Card payments: %d",
                sales.size(), total, cash, card
        );
    }

    public String getTopItems() {
        OffsetDateTime from = LocalDate.now().minusDays(7).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to   = OffsetDateTime.now();
        List<Sale> sales    = saleRepository.findBySoldAtBetween(from, to);

        if (sales.isEmpty()) return "No sales data for the past 7 days.";

        Map<String, BigDecimal> revenueByCategory = sales.stream()
                .collect(Collectors.groupingBy(
                        Sale::getItemCategory,
                        Collectors.reducing(BigDecimal.ZERO, Sale::getPrice, BigDecimal::add)
                ));

        StringBuilder sb = new StringBuilder("Top categories by revenue (last 7 days):\n");
        revenueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append(String.format("  %s: £%.2f%n", e.getKey(), e.getValue())));

        return sb.toString().trim();
    }

    public String getLowStockCategories() {
        OffsetDateTime from = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to   = from.plusDays(1);
        List<Sale> sales    = saleRepository.findBySoldAtBetween(from, to);

        Map<String, Long> countByCategory = sales.stream()
                .collect(Collectors.groupingBy(Sale::getItemCategory, Collectors.counting()));

        List<String> allCategories = List.of("RING", "NECKLACE", "BRACELET", "EARRING");

        List<String> low = allCategories.stream()
                .filter(cat -> countByCategory.getOrDefault(cat, 0L) < 3)
                .collect(Collectors.toList());

        if (low.isEmpty()) return "All categories have healthy sales activity today.";

        return "Categories with low activity today (fewer than 3 sales): " + String.join(", ", low);
    }

    public String getDailyReport(String dateStr) {
        LocalDate date;
        try {
            date = (dateStr == null || dateStr.isBlank()) ? LocalDate.now() : LocalDate.parse(dateStr);
        } catch (Exception e) {
            return "Invalid date format. Use YYYY-MM-DD.";
        }

        Optional<DailyReport> opt = reportRepository.findByReportDate(date);
        if (opt.isEmpty()) return "No report found for " + date + ". Generate one first.";

        DailyReport r = opt.get();
        return String.format(
                "Report for %s: %d transactions | Total: £%.2f | Cash: £%.2f | Card: £%.2f",
                r.getReportDate(), r.getTotalTransactions(),
                r.getTotalRevenue(), r.getCashRevenue(), r.getCardRevenue()
        );
    }

    // ── Tool dispatcher ──────────────────────────────────────────────────────

    public String executeTool(String toolName, JsonNode input) {
        log.info("Agent executing tool: {}", toolName);
        return switch (toolName) {
            case "get_sales_today"          -> getSalesToday();
            case "get_top_items"            -> getTopItems();
            case "get_low_stock_categories" -> getLowStockCategories();
            case "get_daily_report"         -> getDailyReport(
                    input != null ? input.path("date").asText("") : "");
            default -> "Unknown tool: " + toolName;
        };
    }

    // Tool definitions sent to Claude

    public ArrayNode buildToolDefinitions() {
        ArrayNode tools = objectMapper.createArrayNode();

        tools.add(buildTool(
                "get_sales_today",
                "Get a summary of all jewellery sales recorded today, including total revenue and payment method breakdown.",
                objectMapper.createObjectNode()
        ));

        tools.add(buildTool(
                "get_top_items",
                "Get the top 5 best-selling jewellery categories by revenue over the last 7 days.",
                objectMapper.createObjectNode()
        ));

        tools.add(buildTool(
                "get_low_stock_categories",
                "Identify jewellery categories with low sales activity today (fewer than 3 sales) that may need restocking or promotion.",
                objectMapper.createObjectNode()
        ));

        ObjectNode dateSchema = objectMapper.createObjectNode();
        dateSchema.put("type", "object");
        ObjectNode dateProps = dateSchema.putObject("properties");
        ObjectNode dateProp  = dateProps.putObject("date");
        dateProp.put("type", "string");
        dateProp.put("description", "Date to fetch the report for, in YYYY-MM-DD format. Defaults to today.");
        tools.add(buildTool(
                "get_daily_report",
                "Fetch the stored daily sales report for a specific date.",
                dateSchema
        ));

        return tools;
    }

    private ObjectNode buildTool(String name, String description, ObjectNode inputSchema) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);
        if (inputSchema.isEmpty()) {
            ObjectNode empty = objectMapper.createObjectNode();
            empty.put("type", "object");
            empty.putObject("properties");
            tool.set("input_schema", empty);
        } else {
            tool.set("input_schema", inputSchema);
        }
        return tool;
    }
}