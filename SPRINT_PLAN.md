# 10-Day Sprint Plan — Personal Online Bookkeeping System

**Stack:** Vue 3 + Element Plus + Axios + ECharts | Spring Boot 2.x + MyBatis-Plus | MySQL

---

## Day 1 — Project Setup & Database Design
**Goal:** Everything bootstrapped, DB running, schema committed.

- [ ] Create Git repo, set up `.gitignore` for both Maven and Node
- [ ] Design and finalize DB schema (Users, Categories, Transactions, Budgets)
- [ ] Run DDL scripts in MySQL, verify tables in Navicat
- [ ] Initialize Spring Boot project via Spring Initializr (see scaffolding guide)
- [ ] Initialize Vue 3 frontend via Vite
- [ ] Verify both projects start without errors

**Deliverable:** Both apps run locally; DB schema committed to repo.

---

## Day 2 — Backend: User Authentication
**Goal:** Register + Login APIs working with JWT.

- [ ] Implement `User` entity, Mapper, Service, Controller
- [ ] Integrate `jjwt` for JWT token generation/validation
- [ ] Implement `POST /api/auth/register` and `POST /api/auth/login`
- [ ] Add `JwtFilter` to Spring Security filter chain (or a simple servlet filter)
- [ ] Test endpoints with Postman/curl

**Deliverable:** Auth APIs return JWT token on successful login.

---

## Day 3 — Backend: Categories & Transaction Core APIs
**Goal:** CRUD for Categories and Transactions.

- [ ] Implement `Category` entity, Mapper, Service, Controller
  - `GET /api/categories` — list all for current user
  - `POST /api/categories` — create
  - `PUT /api/categories/{id}` — update
  - `DELETE /api/categories/{id}` — delete
- [ ] Implement `Transaction` entity, Mapper, Service, Controller
  - `GET /api/transactions` — list with filters (date range, type, category)
  - `POST /api/transactions` — create
  - `PUT /api/transactions/{id}` — update
  - `DELETE /api/transactions/{id}` — delete
- [ ] Apply JWT authentication to all protected routes
- [ ] Write unified response wrapper (`Result<T>`)

**Deliverable:** All core CRUD APIs secured and tested.

---

## Day 4 — Backend: Statistics & Budget APIs
**Goal:** Aggregate data endpoints that power dashboard charts.

- [ ] `GET /api/stats/summary` — total income, total expense, net balance for a period
- [ ] `GET /api/stats/monthly` — monthly income/expense breakdown (for line/bar chart)
- [ ] `GET /api/stats/category` — expense breakdown by category (for pie chart)
- [ ] Implement `Budget` entity + APIs
  - `GET /api/budgets` — list budgets
  - `POST /api/budgets` — create budget for a category/month
  - `GET /api/budgets/status` — actual vs. budget comparison
- [ ] MyBatis-Plus custom SQL for aggregation queries

**Deliverable:** Stats and budget endpoints return correct aggregated data.

---

## Day 5 — Frontend: Auth Pages & Axios Setup
**Goal:** Login/Register UI integrated with backend.

- [ ] Configure Vue Router (public routes: `/login`, `/register`; protected: everything else)
- [ ] Set up Axios instance with base URL, request interceptor (attach JWT), response interceptor (handle 401)
- [ ] Set up Pinia store for user auth state (token, userInfo)
- [ ] Build `LoginPage.vue` and `RegisterPage.vue` using Element Plus forms
- [ ] Implement navigation guard (`router.beforeEach`) to redirect unauthenticated users
- [ ] Connect forms to `POST /api/auth/login` and `POST /api/auth/register`

**Deliverable:** Can register, log in, and be redirected to dashboard; 401 redirects to login.

---

## Day 6 — Frontend: Transaction Management UI
**Goal:** Full CRUD for transactions in the browser.

- [ ] Build main layout: sidebar navigation + header + content area (`AppLayout.vue`)
- [ ] Build `TransactionListPage.vue`
  - Element Plus Table with pagination
  - Filter bar: date range picker, type selector, category selector
  - Income/Expense row color coding
- [ ] Build `TransactionFormDialog.vue` (Add/Edit dialog)
  - Form fields: amount, type (income/expense), category, date, note
  - Form validation rules
- [ ] Wire up all CRUD operations to backend APIs

**Deliverable:** User can add, view, edit, and delete transactions.

---

## Day 7 — Frontend: Categories & Budget UI
**Goal:** Category management and budget tracking pages.

- [ ] Build `CategoryPage.vue` — table with add/edit/delete
- [ ] Build `BudgetPage.vue`
  - Set monthly budget per category
  - Progress bars showing actual vs. budget (Element Plus `el-progress`)
- [ ] Build `UserProfilePage.vue` — display username, change password form

**Deliverable:** Category and budget pages fully functional.

---

## Day 8 — Frontend: Dashboard & ECharts
**Goal:** Visual dashboard with charts.

- [ ] Build `DashboardPage.vue`
  - Summary cards: Total Income, Total Expense, Net Balance (current month)
  - Line/Bar chart: monthly income vs. expense trend (last 6 months) — ECharts
  - Pie chart: expense breakdown by category — ECharts
  - Recent 5 transactions list
- [ ] Create reusable `BarLineChart.vue` and `PieChart.vue` wrapper components
- [ ] Connect all charts to `/api/stats/*` endpoints
- [ ] Handle loading states and empty states gracefully

**Deliverable:** Dashboard shows live charts from real data.

---

## Day 9 — Integration Testing & Bug Fixing
**Goal:** End-to-end flows work without errors.

- [ ] Full user journey test: Register → Login → Add categories → Add transactions → View dashboard
- [ ] Test budget flow: Set budget → Add transactions → Check budget status page
- [ ] Fix CORS issues (verify Spring Boot CORS config)
- [ ] Fix any date/timezone inconsistencies between frontend and backend
- [ ] Handle edge cases: empty data states, form validation errors, network errors
- [ ] Basic responsive layout check (tablet/desktop)

**Deliverable:** All core user flows work end-to-end without crashes.

---

## Day 10 — Polish, Documentation & Submission
**Goal:** Project is presentable and documented.

- [ ] Clean up console logs, dead code, commented-out blocks
- [ ] Write `README.md`: project overview, tech stack, how to run locally
- [ ] Add sample seed data SQL script (`db/seed.sql`)
- [ ] Final Git commit with clean history
- [ ] Prepare demo: walk through register → dashboard → charts
- [ ] (Optional) Deploy to local Tomcat or Docker Compose for demo

**Deliverable:** Clean, documented, demo-ready project.

---

## Risk & Buffer Notes

| Risk | Mitigation |
|------|-----------|
| JWT auth takes too long | Use Spring Security's built-in form login as fallback |
| ECharts integration issues | Use vue-echarts wrapper library |
| MySQL connection issues | Have H2 in-memory DB as local fallback |
| Running behind on Day 5+ | Skip Budget feature, focus on core transaction CRUD + dashboard |
