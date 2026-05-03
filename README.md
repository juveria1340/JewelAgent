# JewelAgent

> Autonomous AI-Powered Jewellery Sales System — Proof of Concept

A single Spring Boot application that records jewellery sales transactions, streams each sale through Apache Kafka, generates daily revenue reports, archives historical data, and runs an autonomous AI agent powered by Anthropic Claude that analyses live sales data and produces actionable business advisory reports.

---

## Tech Stack

| Technology | Role |
|---|---|
| Spring Boot 3.2 (Java 21) | Application framework — REST API, scheduler, Kafka integration |
| Apache Kafka (KRaft) | Event streaming — no Zookeeper required |
| PostgreSQL 15 | Persistent storage — sales transactions and daily reports |
| Anthropic Claude | Autonomous AI agent with tool use |
| Python 3.11 | Data generator — posts synthetic sales every 8 seconds |
| Docker Compose | Container orchestration |

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- An Anthropic API key from [console.anthropic.com](https://console.anthropic.com)
- No local Java, Kafka, or PostgreSQL installation required

---

## Quick Start

**1. Create a `.env` file in the project root:**

```
ANTHROPIC_API_KEY=sk-ant-api03-...
```

**2. Start all containers:**

```bash
docker compose up --build
```

First run takes 2–3 minutes while Maven downloads dependencies. Subsequent starts are much faster thanks to Docker layer caching.

**3. Verify everything is running:**

```bash
docker ps
```

You should see five containers: `postgres`, `kafka`, `kafka-ui`, `jewelagent-app`, and `data-generator`.

---

## Service URLs

| Service | URL |
|---|---|
| Spring Boot API | http://localhost:8080 |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | localhost:5432 |

**PostgreSQL credentials:** `user` / `password` / database `jewellery`

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/sales` | Record a new sale — saved to DB and published to Kafka |
| `GET` | `/api/sales` | Retrieve all live sales transactions |
| `GET` | `/api/reports` | Retrieve all daily reports, newest first |
| `POST` | `/api/reports/generate` | Generate a daily report (`?date=YYYY-MM-DD`) |
| `POST` | `/api/reports/archive` | Export records older than 7 days to JSON |
| `POST` | `/api/agent/advise` | Run the autonomous agent — returns JSON report |

---

## How It Works

```
data-generator (every 8s)
        │
        ▼
POST /api/sales
        │
        ├──► SaleService saves to PostgreSQL (sales table)
        │
        └──► SaleProducer publishes SaleEvent to Kafka topic
                    │
                    ▼
              SaleConsumer receives event (logs it)

Midnight (scheduled):
  JobScheduler ──► ReportService
    queries sales table → aggregates totals → writes to daily_reports table

01:00 AM (scheduled):
  JobScheduler ──► ArchiveService
    finds records older than 7 days → exports to ./exports/*.json

On demand (you trigger):
  POST /api/agent/advise
    AgentService sends prompt + tool definitions to Claude
    Claude calls tools autonomously:
      ├── get_sales_today          → queries sales table
      ├── get_top_items            → groups by category, last 7 days
      ├── get_low_stock_categories → finds categories with < 3 sales today
      └── get_daily_report         → fetches from daily_reports table
    Claude writes final advisory report
    AgentResponse returned with report + toolCallLog
```

---

## The AI Agent

The agent implements a full agentic loop with tool use. When triggered it does not follow a fixed script — it decides autonomously which tools to call and in what order, based on the prompt. Each tool is a Java method in `AgentToolService` that queries PostgreSQL and returns a plain text result. Claude reads the results, calls more tools if needed, then writes the advisory report.

The response includes:

- `report` — the natural language advisory
- `toolCallLog` — every tool called and what it returned, making the agent's reasoning auditable
- `generatedAt` — ISO-8601 timestamp
- `success` — boolean indicating whether the agent completed successfully

---

## Kafka

Kafka runs in **KRaft mode** — no Zookeeper required. The topic `jewellery.sales` is created automatically on startup with 3 partitions and replication factor 1.

The producer (`SaleProducer`) and consumer (`SaleConsumer`) run inside the Spring Boot app container — not inside the Kafka container. The Kafka container is just the message broker.

Monitor live events at **http://localhost:8090** (Kafka UI).

---

## Scheduler

The scheduler (`JobScheduler`) runs two automatic jobs:

| Job | Default schedule | What it does |
|---|---|---|
| Daily report | Midnight (`0 0 0 * * *`) | Aggregates yesterday's sales into `daily_reports` |
| Archive | 01:00 AM (`0 0 1 * * *`) | Exports records older than 7 days to `./exports/*.json` |

For demo purposes, change the cron expressions in `JobScheduler.java` to run every 2 minutes:

```java
@Scheduled(cron = "0 */2 * * * *")   // every 2 minutes
```
---


## Stopping the System

```bash
# Stop all containers (preserves data)
docker compose down

# Stop and delete all data (fresh start)
docker compose down -v
```

---

## Future Work

- PDF report generation auto-emailed to shop owner daily
- POS terminal integration (Square, Lightspeed)
- Accounting software sync — Xero / QuickBooks
- Multi-branch Kafka topic partitioning
- Predictive analytics — ML-based demand forecasting
- Agent write tools — autonomous stock reordering

---

## Student Information


**Stack:** Spring Boot 3.2 · Apache Kafka (KRaft) · PostgreSQL 15 · Anthropic Claude · Docker Compose
