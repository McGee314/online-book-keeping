# Personal Online Bookkeeping System

A full-stack web application for personal income/expense tracking, built as a university assignment for the "Practice of Software Development" course.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3, Element Plus, Axios, ECharts |
| Backend | Spring Boot 2.7.x, MyBatis-Plus 3.5.x |
| Database | MySQL 5.7 / 8.0 |
| Build | Maven (backend), Vite (frontend) |

## Required Core Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | **User Authentication** | Registration, login, and secure logout with username/password. Username uniqueness verification, encrypted password storage (BCrypt), and route guards to block unauthorized access. |
| 2 | **Bill Management** | Full CRUD for bills (income/expense records). Fixed income/expense categories, reverse chronological sorting, quick filtering by type (income vs. expense). |
| 3 | **Data Statistics** | Real-time monthly total income, expenses, and net balance. Pie chart visualization of expense category distribution for quick consumption pattern identification. |

## Project Documents

| File | Description |
|------|-------------|
| `SPRINT_PLAN.md` | 10-day day-by-day sprint plan |
| `SCAFFOLDING_GUIDE.md` | Step-by-step setup for both projects |
| `PROJECT_STRUCTURE.md` | Folder layout and architecture best practices |
| `db/schema.sql` | MySQL DDL for all tables |
| `db/seed.sql` | Default category seed data |

## Quick Start

### 1. Database

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

### 2. Backend

```bash
cd bookkeeping-backend
# Edit src/main/resources/application.yml with your MySQL credentials
mvn spring-boot:run
# Runs on http://localhost:8080
```

### 3. Frontend

```bash
cd bookkeeping-frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

## API Overview

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/auth/logout` | Logout (clears server-side token if blacklist is implemented) |
| GET | `/api/auth/me` | Get current user info |

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List categories for current user |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |

### Transactions (Bill Management)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | List transactions (reverse chronological, filterable by type/category/date) |
| POST | `/api/transactions` | Create transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

### Data Statistics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stats/summary` | Current month: total income, total expense, net balance |
| GET | `/api/stats/monthly` | Monthly income/expense breakdown (last 6 months, for trend chart) |
| GET | `/api/stats/category` | Expense breakdown by category (for pie chart) |

## Architecture Decision: Stateless JWT Authentication

This project uses JWT (JSON Web Token) for authentication. Tokens are issued at login and must be included in the `Authorization: Bearer <token>` header for all protected requests. Logout is handled client-side by discarding the token from localStorage (with an optional server-side blacklist for additional security). A `JwtAuthInterceptor` acts as a route guard, checking every protected request for a valid token and extracting the user context via `UserContext` (ThreadLocal).