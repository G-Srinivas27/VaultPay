# VaultPay — Project & Docker Session Status

**Last Updated:** October 5, 2026

---

## 🚀 Current Milestone: Full-Stack Dockerization Completed & Pushed to GitHub

All components are containerized, tested, and stored in version control:

1. **Database (`vaultpay-db`)**:
   - PostgreSQL 17 Alpine running on port `5432` with persistent volume `vaultpay_pgdata`.
   - Flyway migrations `V1`..`V5` applied cleanly to the `public` schema.
   - Note: Archived MySQL migrations moved to `src/main/resources/db/_archive_mysql/` to avoid version collisions.
2. **Backend (`vaultpay-backend`)**:
   - Spring Boot 3.5 on Eclipse Temurin Java 21 JRE, port `8080`.
   - Driver: `org.postgresql:postgresql` + `org.flywaydb:flyway-database-postgresql` configured in `pom.xml`.
   - Security: `/actuator/**` and `/v3/api-docs` are permitted in `SecurityConfig.java`.
   - Healthcheck: Verified on `http://localhost:8080/v3/api-docs` (Status: Healthy).
3. **Frontend (`vaultpay-frontend`)**:
   - React application built with Node 22 and served via Nginx Alpine on port `3000`.
   - `nginx.conf` handles SPA routing and reverse-proxies `/api/` calls to `http://backend:8080`.

---

## ⚡ Quick Commands

- **Start Stack:** `docker compose up -d`
- **Check Status:** `docker ps`
- **Stop Stack:** `docker compose down`
- **Follow Logs:** `docker compose logs -f backend`
- **Access Points:**
  - Frontend: http://localhost:3000
  - Backend API: http://localhost:8080
  - Swagger Docs: http://localhost:8080/swagger-ui/index.html
