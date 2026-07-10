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
| 3 | **Category Management** | Full CRUD for income and expense categories, including user-scoped custom categories and system default categories. |

---

## Architecture & How It Works

### 1. System Architecture Overview

```mermaid
graph TB
    subgraph Browser["🖥 Browser"]
        VUE["Vue 3 + Vite<br/>Element Plus UI"]
        STORE["Pinia Store<br/>(User Auth State)"]
        RTR["Vue Router<br/>(Navigation Guard)"]
        AXIOS["Axios Instance<br/>(Interceptors: JWT + 401)"]
    end

    subgraph Backend["⚙ Spring Boot Backend (port 8080)"]
        direction TB
        CORS["CORS Filter"]
        JWT_INT["JwtAuthInterceptor<br/>(Token Validation)"]
        CTRL["Controller Layer<br/>REST Endpoints"]
        SVC["Service Layer<br/>Business Logic"]
        MAPPER["Mapper Layer<br/>MyBatis-Plus"]
        CTX["UserContext<br/>(ThreadLocal)"]
        BCRYPT["BCrypt<br/>Password Hasher"]
    end

    subgraph DB["🗄 MySQL Database"]
        USERS["users"]
        CATS["categories"]
        TXNS["transactions"]
        BUDGETS["budgets"]
    end

    VUE -->|"HTTP /api/*"| AXIOS
    AXIOS -->|"Authorization: Bearer JWT"| CORS
    CORS --> JWT_INT
    JWT_INT -->|"userId → UserContext"| CTX
    JWT_INT --> CTRL
    CTRL --> SVC
    SVC --> MAPPER
    SVC --> CTX
    SVC --> BCRYPT
    MAPPER --> DB
    BCRYPT --> DB

    STORE -.-> AXIOS
    RTR -.-> STORE

    style Browser fill:#e0f2fe,stroke:#38bdf8
    style Backend fill:#dcfce7,stroke:#4ade80
    style DB fill:#fef9c3,stroke:#facc15
```

### 2. Database Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    USERS {
        bigint id PK "Primary key"
        varchar username UK "Unique login name"
        varchar password "BCrypt hashed"
        varchar nickname "Display name"
        varchar email UK "Optional email"
        varchar avatar "Avatar URL"
        tinyint status "1=active, 0=disabled"
        datetime created_at
        datetime updated_at
        tinyint deleted "Soft delete flag"
    }

    CATEGORIES {
        bigint id PK "Primary key"
        bigint user_id FK "NULL = system default"
        varchar name "e.g. Food, Salary"
        tinyint type "1=income, 2=expense"
        varchar icon "Icon identifier"
        varchar color "Hex color"
        int sort_order "Display order"
        datetime created_at
        datetime updated_at
        tinyint deleted "Soft delete"
    }

    TRANSACTIONS {
        bigint id PK "Primary key"
        bigint user_id FK "Owner"
        bigint category_id FK "Category reference"
        tinyint type "1=income, 2=expense"
        decimal amount "Positive value (12,2)"
        date transaction_date "Date occurred"
        varchar note "Optional description"
        datetime created_at
        datetime updated_at
        tinyint deleted "Soft delete"
    }

    BUDGETS {
        bigint id PK "Primary key"
        bigint user_id FK "Owner"
        bigint category_id FK "Target category"
        char year_month "YYYY-MM format"
        decimal amount "Budget limit (12,2)"
        datetime created_at
        datetime updated_at
        tinyint deleted "Soft delete"
    }

    USERS ||--o{ CATEGORIES : "owns (user_id)"
    USERS ||--o{ TRANSACTIONS : "owns (user_id)"
    USERS ||--o{ BUDGETS : "owns (user_id)"
    CATEGORIES ||--o{ TRANSACTIONS : "categorizes (category_id)"
    CATEGORIES ||--o{ BUDGETS : "budgeted by (category_id)"
```

### 3. Authentication Flow

```mermaid
sequenceDiagram
    actor User
    participant Vue as Vue 3 Frontend
    participant Axios as Axios Instance
    participant Auth as AuthController
    participant Svc as UserService
    participant BCrypt as BCrypt Hasher
    participant DB as MySQL (users)
    participant JWT as JWT Util

    Note over User, JWT: === Registration Flow ===

    User->>Vue: Fill Register form
    Vue->>Axios: POST /api/auth/register {username, password, nickname}
    Axios->>Auth: Forward request
    Auth->>Svc: register(RegisterRequest)
    Svc->>DB: Check username exists?
    DB-->>Svc: No duplicate
    Svc->>BCrypt: hashPassword(plainPassword)
    BCrypt-->>Svc: Hashed password
    Svc->>DB: INSERT INTO users
    DB-->>Svc: User saved
    Svc->>JWT: generateToken(userId, username)
    JWT-->>Svc: JWT token string
    Svc-->>Auth: AuthResponse {token, userInfo}
    Auth-->>Vue: Result<AuthResponse>
    Vue->>Vue: localStorage.setItem('token', token)
    Vue->>Vue: router.push('/dashboard')

    Note over User, JWT: === Login Flow ===

    User->>Vue: Fill Login form
    Vue->>Axios: POST /api/auth/login {username, password}
    Axios->>Auth: Forward request
    Auth->>Svc: login(LoginRequest)
    Svc->>DB: SELECT * FROM users WHERE username = ?
    DB-->>Svc: User row (with hashed password)
    Svc->>BCrypt: matches(plainPassword, hashedPassword)
    BCrypt-->>Svc: true
    Svc->>JWT: generateToken(userId, username)
    JWT-->>Svc: JWT token string
    Svc-->>Auth: AuthResponse {token, userInfo}
    Auth-->>Vue: Result<AuthResponse>
    Vue->>Vue: Store token + userInfo
    Vue->>Vue: router.push('/dashboard')
```

### 4. Authenticated Request Lifecycle (Full Flow)

```mermaid
sequenceDiagram
    actor User
    participant Vue as Vue 3 Component
    participant Axios as Axios Interceptors
    participant JwtInt as JwtAuthInterceptor
    participant Uctx as UserContext (ThreadLocal)
    participant Ctrl as TransactionController
    participant Svc as TransactionServiceImpl
    participant Mapper as TransactionMapper (MyBatis-Plus)
    participant DB as MySQL

    Note over User, DB: User wants to view transactions

    User->>Vue: Navigate to /transactions
    Vue->>Axios: GET /api/transactions?page=1&type=2
    Note over Axios: Request Interceptor
    Axios->>Axios: Read token from localStorage
    Axios->>Axios: Set header: Authorization: Bearer <JWT>
    Axios->>JwtInt: HTTP Request with JWT header

    Note over JwtInt: Pre-Handler (before controller)
    JwtInt->>JwtInt: Parse Bearer token from header
    JwtInt->>JwtInt: Validate JWT signature & expiry
    JwtInt->>JwtInt: Extract userId & username from claims
    JwtInt->>Uctx: UserContext.setUserId(userId)
    Note over Uctx: Stored in ThreadLocal<br/>(safe per-request isolation)
    JwtInt->>Ctrl: proceed to controller

    Ctrl->>Svc: listTransactions(TransactionQueryRequest)
    Svc->>Uctx: Long userId = UserContext.getUserId()
    Uctx-->>Svc: current userId
    Svc->>Svc: Build LambdaQueryWrapper<br/>.eq(user_id, userId)<br/>.eq(type, request.type)<br/>.between(date, start, end)
    Svc->>Mapper: selectPage(page, wrapper)
    Mapper->>DB: SELECT * FROM transactions<br/>WHERE user_id = ? AND deleted = 0<br/>ORDER BY transaction_date DESC<br/>LIMIT ?, ?
    DB-->>Mapper: ResultSet
    Mapper-->>Svc: Page<Transaction>
    Svc-->>Ctrl: Page result
    Ctrl-->>Axios: Result<Page<Transaction>>
    Note over Axios: Response Interceptor
    Axios->>Axios: Extract response.data (unwrap Result)
    Axios-->>Vue: {code: 200, data: {records: [...], total: 42}}

    Note over Axios: If 401 received:
    Axios-->>Axios: Clear localStorage token
    Axios-->>Vue: router.push('/login')
    Axios-->>Vue: ElMessage.error('Session expired')

    Note over Uctx: After request completes<br/>ThreadLocal is cleared<br/>(prevents memory leaks)
```

### 5. Frontend Component Tree & Routing

```mermaid
graph TB
    subgraph APP["App.vue (Root)"]
        direction TB
        ROUTER["Vue Router"]
        ROUTER --> PUBLIC["Public Routes<br/>(no auth required)"]
        ROUTER --> PROTECTED["Protected Routes<br/>(auth guard: router.beforeEach)"]

        PUBLIC --> LOGIN["/login<br/>LoginPage.vue"]
        PUBLIC --> REGISTER["/register<br/>RegisterPage.vue"]

        PROTECTED --> LAYOUT["AppLayout.vue<br/>(Sidebar + Header + Content)"]

        subgraph LAYOUT_INNER["Layout Children"]
            DASHBOARD["/dashboard<br/>DashboardPage.vue"]
            T_LIST["/transactions<br/>TransactionListPage.vue"]
            CAT_PAGE["/categories<br/>CategoryPage.vue"]
            PROFILE["/profile<br/>UserProfilePage.vue"]
        end

        LAYOUT --> DASHBOARD
        LAYOUT --> T_LIST
        LAYOUT --> CAT_PAGE
        LAYOUT --> PROFILE
    end

    subgraph COMPONENTS["Reusable Components"]
        TXN_DIALOG["TransactionFormDialog.vue"]
        SUMMARY["SummaryCard.vue"]
    end

    T_LIST -.-> TXN_DIALOG
    DASHBOARD -.-> SUMMARY

    subgraph STORES["Pinia Stores"]
        USER_STORE["useUserStore<br/>(token, userInfo, login, logout)"]
        APP_STORE["useAppStore<br/>(sidebarCollapsed, ...)"]
    end

    subgraph API["API Layer (src/api/)"]
        AUTH_API["auth.js"]
        TXN_API["transaction.js"]
        CAT_API["category.js"]
    end

    subgraph CORE["Core Utilities"]
        REQUEST["request.js<br/>(Axios instance)"]
        FORMAT["format.js<br/>(currency, date)"]
    end

    LAYOUT_INNER --> API
    API --> REQUEST
    LAYOUT_INNER --> STORES
    STORES -.-> REQUEST

    style APP fill:#e0f2fe,stroke:#38bdf8
    style PUBLIC fill:#fff3cd,stroke:#facc15
    style PROTECTED fill:#d1fae5,stroke:#4ade80
    style COMPONENTS fill:#fce7f3,stroke:#f472b6
    style STORES fill:#ede9fe,stroke:#a78bfa
    style API fill:#fef3c7,stroke:#f59e0b
    style CORE fill:#e5e7eb,stroke:#6b7280
```

### 6. Backend Package & Layer Diagram

```mermaid
graph LR
    subgraph REQ["Incoming HTTP Request"]
    end

    subgraph FILTER["Filter / Interceptor Layer"]
        CORS["CorsFilter<br/>(@Configuration)"]
        JWT_F["JwtAuthInterceptor<br/>(implements HandlerInterceptor)"]
    end

    subgraph CTRL_LAYER["Controller Layer (REST)"]
        AUTH_CTRL["AuthController<br/>/api/auth/*"]
        CAT_CTRL["CategoryController<br/>/api/categories/*"]
        TXN_CTRL["TransactionController<br/>/api/transactions/*"]
    end

    subgraph SVC_LAYER["Service Layer (Business Logic)"]
        USER_SVC["UserServiceImpl"]
        CAT_SVC["CategoryServiceImpl"]
        TXN_SVC["TransactionServiceImpl"]
    end

    subgraph INFRA["Infrastructure / Cross-Cutting"]
        JWT_UTIL["JwtUtil<br/>(generate / parse)"]
        PWD_CFG["PasswordConfig<br/>(BCrypt bean)"]
        UCTX["UserContext<br/>(ThreadLocal<Long>)"]
        EX_HANDLER["GlobalExceptionHandler<br/>(@RestControllerAdvice)"]
        RESULT["Result<T><br/>(unified response)"]
    end

    subgraph DATA["Data Access Layer"]
        USER_M["UserMapper<br/>extends BaseMapper"]
        CAT_M["CategoryMapper<br/>extends BaseMapper"]
        TXN_M["TransactionMapper<br/>extends BaseMapper"]
        BUD_M["BudgetMapper<br/>extends BaseMapper"]
    end

    subgraph DB_L["MySQL Database"]
    end

    REQ --> CORS
    CORS --> JWT_F
    JWT_F --> CTRL_LAYER
    CTRL_LAYER --> SVC_LAYER
    SVC_LAYER --> DATA
    SVC_LAYER --> INFRA
    JWT_F --> INFRA
    DATA --> DB_L

    style REQ fill:#fce7f3,stroke:#ec4899
    style FILTER fill:#fff7ed,stroke:#f97316
    style CTRL_LAYER fill:#e0f2fe,stroke:#38bdf8
    style SVC_LAYER fill:#dcfce7,stroke:#4ade80
    style INFRA fill:#fef3c7,stroke:#f59e0b
    style DATA fill:#ede9fe,stroke:#a78bfa
    style DB_L fill:#f0fdf4,stroke:#22c55e
```

---

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
| GET | `/api/categories` | List categories |
| GET | `/api/transactions` | List transactions (paginated, filterable) |
| POST | `/api/transactions` | Create transaction |
| PUT | `/api/transactions/{id}` | Update transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

The repository also contains the `budgets` table in `db/schema.sql` for future expansion, but there is no corresponding REST controller in the current backend.
