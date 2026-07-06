# 10-Day Sprint Plan — Personal Online Bookkeeping System

**Stack:** Vue 3 + Element Plus + Axios + ECharts | Spring Boot 2.x + MyBatis-Plus | MySQL

**Required Core Features (Must Complete):**
1. **User Authentication** — registration, login, secure logout; username uniqueness verification; encrypted password storage; route guards to block unauthorized access.
2. **Bill Management** — CRUD bills; fixed income/expense categories; reverse chronological sort; quick filtering by type.
3. **Data Statistics** — real-time monthly total income/expenses/balance; pie chart to visualize expense category distribution for quick consumption pattern identification.

---

## Day 1 — Project Setup & Database Design
**Goal:** Everything bootstrapped, DB running, schema committed.

- [ ] Create Git repo, set up `.gitignore` for both Maven and Node
- [ ] Design and finalize DB schema (Users, Categories, Transactions)
- [ ] Run DDL scripts in MySQL, verify tables in Navicat
- [ ] Initialize Spring Boot project via Spring Initializr (see scaffolding guide)
- [ ] Initialize Vue 3 frontend via Vite
- [ ] Verify both projects start without errors

**Deliverable:** Both apps run locally; DB schema committed to repo.

---

## Day 2 — Backend: User Authentication
**Goal:** Register + Login + Logout APIs working with JWT.

- [ ] Implement `User` entity, Mapper, Service, Controller
- [ ] Integrate `jjwt` for JWT token generation/validation
- [ ] Implement `POST /api/auth/register` and `POST /api/auth/login`
- [ ] Implement `POST /api/auth/logout` (client-side token discard; optionally server-side token blacklist)
- [ ] Add `JwtAuthInterceptor` to enforce route guards on protected endpoints
- [ ] Test endpoints with Postman/curl

**Deliverable:** Auth APIs return JWT on login, block unauthorized access, support logout.

---

## Day 3 — Backend: Categories & Transaction Core APIs
**Goal:** CRUD for Categories and Transactions.

- [ ] Implement `Category` entity, Mapper, Service, Controller
  - `GET /api/categories` — list all for current user
  - `POST /api/categories` — create
  - `PUT /api/categories/{id}` — update
  - `DELETE /api/categories/{id}` — delete
- [ ] Implement `Transaction` entity, Mapper, Service, Controller
  - `GET /api/transactions` — list with filters (date range, type, category), ordered reverse chronological
  - `POST /api/transactions` — create
  - `PUT /api/transactions/{id}` — update
  - `DELETE /api/transactions/{id}` — delete
- [ ] Apply JWT authentication to all protected routes
- [ ] Write unified response wrapper (`Result<T>`)

**Deliverable:** All core CRUD APIs secured and tested.

---

## Day 4 — Backend: Data Statistics API
**Goal:** Aggregate data endpoints that power the dashboard statistics (real-time monthly totals and pie chart).

- [ ] `GET /api/stats/summary` — total income, total expense, net balance for current month
- [ ] `GET /api/stats/monthly` — monthly income/expense breakdown (for line/bar chart, last 6 months)
- [ ] `GET /api/stats/category` — expense breakdown by category (for pie chart)
- [ ] MyBatis custom SQL for aggregation queries (GROUP BY month, GROUP BY category)
- [ ] Write integration tests to verify stats recalculate after transaction deletion

**Acceptance Criteria (from requirements):** Statistics must update after bill deletion; data consistency is critical.

**Deliverable:** Stats endpoints return correct aggregated data that stays consistent with transactions.

---

## Day 5 — Frontend: Auth Pages & Axios Setup
**Goal:** Login/Register UI integrated with backend.

- [ ] Configure Vue Router (public routes: `/login`, `/register`; protected: everything else)
- [ ] Set up Axios instance with base URL, request interceptor (attach JWT), response interceptor (handle 401)
- [ ] Set up Pinia store for user auth state (token, userInfo)
- [ ] Build `LoginPage.vue` and `RegisterPage.vue` using Element Plus forms
- [ ] Implement navigation guard (`router.beforeEach`) to redirect unauthenticated users
- [ ] Connect forms to `POST /api/auth/login` and `POST /api/auth/register`
- [ ] Implement logout: clear token from localStorage and redirect to login

**Deliverable:** Can register, log in, and be redirected to dashboard; 401 redirects to login; logout works.

---

## Day 6 — Frontend: Transaction Management UI
**Goal:** Full CRUD for transactions in the browser.

- [ ] Build main layout: sidebar navigation + header + content area (`AppLayout.vue`)
- [ ] Build `TransactionListPage.vue`
  - Element Plus Table with pagination
  - Filter bar: date range picker, type selector, category selector
  - Income/Expense row color coding
  - Reverse chronological order by default
- [ ] Build `TransactionFormDialog.vue` (Add/Edit dialog)
  - Form fields: amount, type (income/expense), category, date, note
  - Form validation rules with clear prompts (acceptance criteria)
- [ ] Wire up all CRUD operations to backend APIs

**Deliverable:** User can add, view, edit, and delete transactions; sorting and filtering work.

---

## Day 7 — Frontend: Categories Management UI
**Goal:** Category management page.

- [ ] Build `CategoryPage.vue` — table with add/edit/delete
- [ ] Display fixed income/expense categories with type indicators
- [ ] Build `UserProfilePage.vue` — display username, change password form
- [ ] Clean form validation and user experience details

**Deliverable:** Category page fully functional with proper feedback.

---

## Day 8 — Frontend: Dashboard & ECharts (Data Statistics)
**Goal:** Visual dashboard with charts matching the Required Core Features.

- [ ] Build `DashboardPage.vue`
  - Summary cards: Total Income, Total Expense, Net Balance (current month — real-time)
  - Line/Bar chart: monthly income vs. expense trend (last 6 months) — ECharts
  - Pie chart: expense breakdown by category — ECharts (required core feature)
  - Recent 5 transactions list
- [ ] Create reusable `BarLineChart.vue` and `PieChart.vue` wrapper components
- [ ] Connect all charts to `/api/stats/*` endpoints
- [ ] Handle loading states and empty states gracefully

**Deliverable:** Dashboard shows live charts from real data; statistics update in real-time.

---

## Day 9 — Integration Testing & Bug Fixing
**Goal:** End-to-end flows work without errors. Focus on data consistency.

- [ ] Full user journey test: Register → Login → Add categories → Add transactions → View dashboard
- [ ] Verify statistics update after bill deletion (acceptance criteria)
- [ ] Fix CORS issues (verify Spring Boot CORS config)
- [ ] Fix any date/timezone inconsistencies between frontend and backend
- [ ] Handle edge cases: empty data states, form validation errors, network errors
- [ ] Basic responsive layout check (tablet/desktop)

**Deliverable:** All 3 core user flows work end-to-end without crashes; data stays consistent.

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
| JWT auth takes too long | Use a simple servlet filter / interceptor approach instead of full Spring Security |
| ECharts integration issues | Use vue-echarts wrapper library |
| MySQL connection issues | Have H2 in-memory DB as local fallback |
| Running behind on Day 5+ | Prioritize the 3 Required Core Features; defer enhancements |
| Statistics data inconsistency | Write integration tests early (Day 4); verify after every transaction mutation |