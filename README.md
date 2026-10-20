# Application Tracker

Application Tracker is an intelligent job-application tracking monorepo that reads your Gmail, automatically classifies job confirmation and status update emails, and visualizes your active job applications on a modern dashboard.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3 (`3.3.4`), Spring Web, Spring Data JPA, Spring Security (OAuth2 client), PostgreSQL, Flyway migrations, JUnit 5, Mockito, Testcontainers.
- **Frontend**: React 19, Vite, Tailwind CSS, Lucide Icons.
- **Infrastructure**: Docker Compose (PostgreSQL 16).

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

### 3. Start PostgreSQL Database
```bash
docker-compose up -d
```
The database will be accessible on `localhost:5432` with database name `application_tracker`.

### 4. Run the Backend
```bash
cd backend
./mvnw spring-boot:run
```
*(On Windows PowerShell, use `.\mvnw.cmd spring-boot:run` or `mvn spring-boot:run`)*

Verify that the health endpoint is responding:
```bash
curl http://localhost:8080/api/health
```
Response:
```json
{
  "status": "UP",
  "message": "Application Tracker API is running",
  "timestamp": "2026-10-20T14:40:00Z"
}
```

### 5. Run the Frontend
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser to view the dashboard and live backend health status.

---

## Running Tests

### Backend Tests
```bash
cd backend
./mvnw clean test
```
The test suite includes:
- `@WebMvcTest` verifying `GET /api/health` status, payload, and headers.
- `@SpringBootTest` verifying Spring Boot application context initialization with H2 in-memory profile.

### Frontend Build & Lint
```bash
cd frontend
npm run build
```

---

## Security Guidelines

- Never commit `.env`, `credentials*.json`, `token*.json`, or any secret keys.
- Gmail integration must strictly request the minimal required readonly scope: `https://www.googleapis.com/auth/gmail.readonly`.