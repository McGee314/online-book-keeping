# Personal Online Bookkeeping System

A full-stack web application for personal income/expense tracking, built as a university assignment for the "Practice of Software Development" course.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3, Element Plus, Axios, ECharts |
| Backend | Spring Boot 2.7.x, MyBatis-Plus 3.5.x |
| Database | MySQL 5.7 / 8.0 |
| Build | Maven (backend), Vite (frontend) |

## Features

- User registration and login with JWT authentication
- Income and expense transaction management (CRUD + filters)
- Custom categories per user
- Monthly budget tracking with actual vs. budget comparison
- Dashboard with summary cards and ECharts visualizations
  - Monthly income/expense trend (bar/line chart)
  - Expense breakdown by category (pie chart)

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

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/categories` | List categories |
| GET | `/api/transactions` | List transactions (paginated, filterable) |
| POST | `/api/transactions` | Create transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |
| GET | `/api/stats/summary` | Income/expense/balance totals |
| GET | `/api/stats/monthly` | Monthly breakdown for charts |
| GET | `/api/stats/category` | Per-category totals for pie chart |
| GET | `/api/budgets` | List budgets |
| POST | `/api/budgets` | Set monthly budget |
| GET | `/api/budgets/status` | Actual vs. budget comparison |
