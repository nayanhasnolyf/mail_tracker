# Application Tracker

[![CI](https://github.com/nayanhasnolyf/mail_tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/nayanhasnolyf/mail_tracker/actions/workflows/ci.yml)

Application Tracker is an intelligent job-application tracking monorepo that reads your Gmail, automatically classifies job confirmation and status update emails, and visualizes your active job applications on a modern dashboard.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3 (`3.3.4`), Spring Web, Spring Data JPA, Spring Security (OAuth2 client), PostgreSQL, Flyway migrations, JUnit 5, Mockito, Testcontainers.
- **Frontend**: React 19, Vite, Tailwind CSS, Lucide Icons, Recharts.
- **Infrastructure & CI**: Docker, Docker Compose (PostgreSQL 16, Spring Boot API, Nginx SPA), GitHub Actions CI.

---

## Monorepo Structure

```text
mail_tracker/
├── backend/                              # Spring Boot 3 API service
│   ├── .mvn/wrapper/                     # Maven wrapper configuration
│   ├── src/main/java/com/applicationtracker/
│   │   ├── config/                       # Security & CORS configuration
│   │   ├── controller/                   # REST controllers (HealthController: GET /api/health)
│   │   ├── domain/                       # JPA entities
│   │   ├── dto/                          # Data transfer objects
│   │   ├── repository/                   # Spring Data JPA repositories
│   │   ├── service/                      # Business logic & email parsing services
│   │   └── ApplicationTrackerApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/                 # Flyway SQL migrations (V1__init.sql)
│   │   └── application.yml               # Application configuration
│   ├── src/test/                         # JUnit 5 & MockMvc test suite
│   ├── mvnw / mvnw.cmd                   # Maven wrapper scripts
│   └── pom.xml
├── frontend/                             # React + Vite + Tailwind CSS dashboard
│   ├── src/
│   │   ├── App.jsx                       # Interactive dashboard with live health check
│   │   ├── index.css                     # Tailwind CSS entrypoint
│   │   └── main.jsx
│   ├── package.json
│   ├── tailwind.config.js
│   └── vite.config.js                    # Vite configuration with /api reverse proxy
├── docker-compose.yml                    # Local PostgreSQL service
├── .env.example                          # Environment variable template
└── .gitignore                            # Root ignore rules for secrets and build artifacts
```

---

## Quick Start & Local Development

### 1. Prerequisites
- **Java 21+**
- **Maven 3.9+** (or use included `./mvnw`)
- **Node.js 20+** & **npm 10+**
- **Docker & Docker Compose** (for local PostgreSQL database)

### 2. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

### 3. Option A: Full-Stack Docker Compose (Recommended)
Build and run the entire stack (PostgreSQL database, Spring Boot backend API, and Nginx frontend dashboard) with a single command:
```bash
docker-compose up --build
```
- **Frontend Dashboard**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080/api/health](http://localhost:8080/api/health)
- **PostgreSQL**: `localhost:5432`

---

### 4. Option B: Local Development

#### Start PostgreSQL Database Only
```bash
docker-compose up -d postgres
```

#### Run the Backend
```bash
cd backend
mvn spring-boot:run
```
*(Or use `./mvnw spring-boot:run`)*

#### Run the Frontend
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## Running Tests & CI

### Backend Tests (JUnit 5, Mockito & Testcontainers)
```bash
cd backend
mvn clean test
```
Runs the full unit, repository (Testcontainers PostgreSQL), and controller MockMvc test suite.

### Frontend Lint, Tests & Production Build
```bash
cd frontend
npm run lint    # Runs oxlint
npm test        # Runs Vitest & React Testing Library component tests
npm run build   # Validates production Vite compilation
```

---

## Security Guidelines

- Never commit `.env`, `credentials*.json`, `token*.json`, or any secret keys.
- Gmail integration must strictly request the minimal required readonly scope: `https://www.googleapis.com/auth/gmail.readonly`.