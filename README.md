# LedgerFlow — Multi-Branch Banking & ACH Settlement Platform

A scaled-down, architecturally faithful core-banking system demonstrating:

- Multiple simulated branches with unified account management
- Double-entry bookkeeping with auditable ledger
- Atomic fund transfers with ACID transaction guarantees
- Concurrency control preventing double-spending
- ACH-style batch settlement with netting
- Saga/compensating transactions for failure recovery

## Team

| Role | Responsibilities |
|------|-----------------|
| **Person A** (Core Banking Engineer) | Branch/account management, double-entry ledger, fund transfers, concurrency control, ACH settlement, saga compensation, core tests |
| **Person B** (Platform Engineer) | JWT auth, RBAC, React dashboard, reconciliation, statements, deployment, integration/load testing |

## Tech Stack

| Technology | Purpose |
|-----------|---------|
| Java 17+ | Programming language |
| Spring Boot 3.2 | Application framework |
| Maven | Build tool (via wrapper) |
| PostgreSQL 16 | Database |
| Spring Data JPA | ORM / Database access |
| Docker Compose | Database containerization |
| JUnit 5 + Mockito | Testing |

## Prerequisites

- **Java 17+** JDK ([Adoptium](https://adoptium.net/))
- **Docker Desktop** ([Docker](https://www.docker.com/products/docker-desktop/))
- Maven is NOT required — the project includes a Maven Wrapper (`mvnw`)

## Quick Start

### 1. Start the database
```bash
docker-compose up -d
```

### 2. Build and run tests
```bash
./mvnw test
```

### 3. Start the application
```bash
./mvnw spring-boot:run
```

### 4. Verify
Open [http://localhost:8080/api/health](http://localhost:8080/api/health) in your browser.

### 5. Stop
- Application: `Ctrl+C`
- Database: `docker-compose down`

## Project Structure

```
src/
├── main/
│   ├── java/com/ledgerflow/
│   │   ├── LedgerFlowApplication.java    # Entry point
│   │   ├── controller/                   # REST API controllers
│   │   ├── service/                      # Business logic (coming soon)
│   │   ├── repository/                   # Database access (coming soon)
│   │   ├── entity/                       # JPA entities (coming soon)
│   │   └── dto/                          # Data transfer objects (coming soon)
│   └── resources/
│       ├── application.yml               # App config
│       └── application-test.yml          # Test config (H2)
└── test/
    └── java/com/ledgerflow/              # Tests
```

## API Endpoints

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | `/api/health` | Health check | ✅ Available |

_More endpoints will be added in subsequent phases._
