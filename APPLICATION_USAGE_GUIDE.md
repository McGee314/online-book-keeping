# Application Usage Guide — Personal Online Bookkeeping System

This guide covers how to use, run, and interact with the personal online bookkeeping application from both the **backend** (Spring Boot REST API) and **frontend** (Vue 3 SPA) perspectives.

---

## Table of Contents

- [1. System Overview](#1-system-overview)
- [2. Prerequisites](#2-prerequisites)
- [3. Database Setup](#3-database-setup)
- [4. Backend Usage](#4-backend-usage)
  - [4.1 Configuration](#41-configuration)
  - [4.2 Starting the Backend](#42-starting-the-backend)
  - [4.3 Backend Architecture & Request Lifecycle](#43-backend-architecture--request-lifecycle)
  - [4.4 Complete API Reference](#44-complete-api-reference)
    - [Auth Endpoints](#auth-endpoints)
    - [Category Endpoints](#category-endpoints)
    - [Transaction Endpoints](#transaction-endpoints)
  - [4.5 Unified Response Format](#45-unified-response-format)
  - [4.6 Error Handling](#46-error-handling)
- [5. Frontend Usage](#5-frontend-usage)
  - [5.1 Configuration](#51-configuration)
  - [5.2 Starting the Frontend](#52-starting-the-frontend)
  - [5.3 Frontend Architecture](#53-frontend-architecture)
  - [5.4 Route Structure](#54-route-structure)
  - [5.5 API Layer Pattern](#55-api-layer-pattern)
  - [5.6 Auth Flow (Frontend)](#56-auth-flow-frontend)
- [6. Docker Deployment](#6-docker-deployment)
- [7. Running Tests](#7-running-tests)

---

## 1. System Overview

```
┌──────────────────────┐     HTTP/REST      ┌──────────────────────┐     SQL      ┌──────────┐
│   Vue 3 SPA          │ ──────────────────> │   Spring Boot        │ ──────────> │  MySQL   │
│   (port 5173 dev)    │ <── JSON/Result<T>  │   (port 8080)        │ <── JDBC    │          │
│   Element Plus UI    │                     │   MyBatis-Plus       │             │  4 tables│
│   ECharts            │  Authorization:     │   JWT Auth           │             └──────────┘
│   Pinia Store        │  Bearer <token>     │   BCrypt Hasher      │
└──────────────────────┘                     └──────────────────────┘
```

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 (Composition API), Element Plus, Axios, ECharts, Pinia, Vue Router |
| Backend | Spring Boot 2.7.x, MyBatis-Plus 3.5.x, JWT (jjwt), BCrypt |
| Database | MySQL 5.7 / 8.0, utf8mb4 |

---

## 2. Prerequisites

- **Java 17+** (for Spring Boot)
- **Maven 3.8+** (or use the included `mvnw` wrapper)
- **Node.js 18+** and **npm 9+** (for Vue/Vite)
- **MySQL 5.7 or 8.0** (running on localhost or accessible network)
- **Docker & Docker Compose** (optional, for containerized deployment)

---

## 3. Database Setup

### 3.1 Create the Database and Tables

Run the provided SQL scripts against your MySQL instance:

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

`db/schema.sql` creates the `bookkeeping_db` database and 4 tables:

| Table | Purpose | Key Fields |
|-------|---------|------------|
| `users` | User accounts | id, username (unique), password (BCrypt hashed), nickname, email, status |
| `categories` | Income/expense categories | id, user_id (FK, NULL=system default), name, type (1=income, 2=expense), icon, color |
| `transactions` | Bill records | id, user_id (FK), category_id (FK), type (1=income, 2=expense), amount, transaction_date, note |
| `budgets` | Budget limits (future expansion, no API yet) | id, user_id (FK), category_id (FK), year_month, amount |

All tables use **soft delete** (`deleted` flag, managed by MyBatis-Plus `@TableLogic`).

`db/seed.sql` inserts default system categories (user_id = NULL):
- Income: Salary, Bonus, Investment, Part-time, Other Income
- Expense: Food & Dining, Transport, Shopping, Entertainment, Housing, Medical, Education, Other Expense

### 3.2 Verify

```bash
mysql -u root -p -e "USE bookkeeping_db; SHOW TABLES; SELECT COUNT(*) FROM categories;"
# Expected: 13 default categories
```

---

## 4. Backend Usage

### 4.1 Configuration

Edit `bookkeeping-backend/src/main/resources/application.yml` to match your environment:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookkeeping_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root           # <-- CHANGE to your MySQL username
    password: your_password  # <-- CHANGE to your MySQL password
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL logging enabled

jwt:
  secret: your-secret-key-at-least-256-bits-long-for-hs256  # <-- CHANGE in production
  expiration: 86400000  # 24 hours in milliseconds
```

Key config notes:
- **`spring.datasource.url`**: Update host, port, and timezone as needed.
- **`spring.datasource.username/password`**: Must match your MySQL credentials.
- **`jwt.secret`**: Change this to a long random string for production. The HS256 algorithm requires at least 256 bits.
- **`jwt.expiration`**: Token lifetime in milliseconds (default 86400000 = 24 hours).

### 4.2 Starting the Backend

**Option A: Using Maven Wrapper (no Maven install needed)**

```bash
cd bookkeeping-backend
./mvnw spring-boot:run
```

**Option B: Using installed Maven**

```bash
cd bookkeeping-backend
mvn spring-boot:run
```

**Option C: Build JAR then run**

```bash
cd bookkeeping-backend
./mvnw clean package -DskipTests
java -jar target/bookkeeping-backend-*.jar
```

The backend starts on **http://localhost:8080**.  
Verify: Open http://localhost:8080/ — you should see the API documentation/demo page (served from `src/main/resources/static/index.html`).

### 4.3 Backend Architecture & Request Lifecycle

Understanding the request flow is essential for using and debugging the API:

```
Client Request
    │
    ▼
┌─────────────────┐
│  CORS Filter     │  ← Allows cross-origin requests from frontend (localhost:5173)
└────────┬────────┘
         ▼
┌─────────────────────┐
│  JwtAuthInterceptor  │  ← Intercepts ALL requests EXCEPT /api/auth/register and /api/auth/login
│  (HandlerInterceptor)│     1. Extracts "Bearer <token>" from Authorization header
└────────┬────────────┘     2. Validates JWT signature & expiry
         │                   3. Extracts userId from token claims
         ▼                   4. Sets UserContext.setUserId(userId) → ThreadLocal
┌─────────────────┐         If invalid/missing: returns 401 Unauthorized
│  Controller      │
│  (REST endpoints)│  ← Validates input, calls service, returns Result<T>
└────────┬────────┘
         ▼
┌─────────────────┐
│  Service Layer   │  ← Business logic. Calls UserContext.getUserId() to get current user.
│  (Business logic)│     This ThreadLocal pattern prevents users from accessing each other's data.
└────────┬────────┘     All queries include WHERE user_id = <currentUserId>.
         ▼
┌─────────────────┐
│  Mapper Layer    │  ← MyBatis-Plus BaseMapper. Automatic CRUD, plus custom SQL if needed.
│  (Data access)   │
└────────┬────────┘
         ▼
┌─────────────────┐
│  MySQL Database  │
└─────────────────┘
```

**Key principle**: Services get the current user ID from `UserContext.getUserId()` — never from a request parameter. This ensures data isolation between users.

### 4.4 Complete API Reference

All endpoints are prefixed with `/api`. All responses are wrapped in `Result<T>` (see section 4.5).

#### Auth Endpoints

**Base path**: `/api/auth`

---

##### `POST /api/auth/register` — Register a new user

- **Auth required**: No
- **Request body** (JSON):
  ```json
  {
    "username": "john_doe",        // Required, String, unique
    "password": "securePass123",   // Required, String, min 6 chars
    "nickname": "John"             // Optional, String
  }
  ```
- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "userInfo": {
        "id": 1,
        "username": "john_doe",
        "nickname": "John",
        "email": null,
        "avatar": null
      }
    }
  }
  ```
- **Error responses**:
  - `400`: Username already exists → `"Username already exists"`
  - `400`: Validation errors (e.g., short password)

---

##### `POST /api/auth/login` — Login

- **Auth required**: No
- **Request body** (JSON):
  ```json
  {
    "username": "john_doe",       // Required, String
    "password": "securePass123"   // Required, String
  }
  ```
- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "userInfo": {
        "id": 1,
        "username": "john_doe",
        "nickname": "John",
        "email": null,
        "avatar": null
      }
    }
  }
  ```
- **Error responses**:
  - `400`: Wrong username or password → `"Invalid username or password"`
  - `400`: Account disabled

---

##### `POST /api/auth/logout` — Logout

- **Auth required**: Yes (Bearer token in `Authorization` header)
- **Request body**: None
- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "Logout successful",
    "data": null
  }
  ```
- **Note**: This is a server-side notification. The frontend also clears the token from localStorage independently.

---

##### `GET /api/auth/me` — Get current user info

- **Auth required**: Yes
- **Request params**: None
- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "username": "john_doe",
      "nickname": "John",
      "email": "john@example.com",
      "avatar": null
    }
  }
  ```

---

#### Category Endpoints

**Base path**: `/api/categories`

---

##### `GET /api/categories` — List user's categories

- **Auth required**: Yes
- **Query parameters** (optional):
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `type` | Integer | (all) | Filter: `1` = income, `2` = expense |

- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "userId": 1,
        "name": "Food & Dining",
        "type": 2,
        "icon": "food",
        "color": "#FF6B6B",
        "sortOrder": 0,
        "createdAt": "2024-01-01T00:00:00"
      },
      {
        "id": 2,
        "userId": null,
        "name": "Salary",
        "type": 1,
        "icon": "salary",
        "color": "#4ECDC4",
        "sortOrder": 0,
        "createdAt": "2024-01-01T00:00:00"
      }
    ]
  }
  ```
- **Note**: Returns both user's custom categories AND system default categories (where `userId = null`).

---

##### `POST /api/categories` — Create a category

- **Auth required**: Yes
- **Request body** (JSON):
  ```json
  {
    "name": "Freelance",     // Required, String
    "type": 1,               // Required, Integer: 1=income, 2=expense
    "icon": "laptop",        // Optional, String (icon identifier)
    "color": "#45B7D1"       // Optional, String (hex color)
  }
  ```
- **Success response (200)**: Returns the created `Category` object.
- **Note**: The category is automatically scoped to the authenticated user (via `UserContext`).

---

##### `PUT /api/categories/{id}` — Update a category

- **Auth required**: Yes
- **Path variable**: `id` — category ID
- **Request body** (JSON):
  ```json
  {
    "name": "Side Hustle",   // Optional (only include fields to update)
    "icon": "money",
    "color": "#2ECC71"
  }
  ```
- **Success response (200)**: Returns the updated `Category` object.
- **Error**: `403` if trying to edit a system default category (userId = null).

---

##### `DELETE /api/categories/{id}` — Delete a category

- **Auth required**: Yes
- **Path variable**: `id` — category ID
- **Success response (200)**:
  ```json
  { "code": 200, "message": "success", "data": null }
  ```
- **Error**: `403` if trying to delete a system default category.

---

#### Transaction Endpoints

**Base path**: `/api/transactions`

---

##### `GET /api/transactions` — List transactions (paginated, filterable)

- **Auth required**: Yes
- **Query parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `page` | Integer | `1` | Page number (1-indexed) |
  | `size` | Integer | `10` | Page size (items per page) |
  | `type` | Integer | (all) | Filter: `1` = income, `2` = expense |
  | `categoryId` | Long | (all) | Filter by specific category |
  | `startDate` | String (yyyy-MM-dd) | (none) | Start of date range |
  | `endDate` | String (yyyy-MM-dd) | (none) | End of date range |

- **Success response (200)**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "records": [
        {
          "id": 1,
          "userId": 1,
          "categoryId": 5,
          "type": 2,
          "amount": 35.50,
          "transactionDate": "2024-03-15",
          "note": "Lunch at restaurant",
          "createdAt": "2024-03-15T12:30:00",
          "category": {
            "id": 5,
            "name": "Food & Dining",
            "icon": "food",
            "color": "#FF6B6B"
          }
        }
      ],
      "total": 42,
      "size": 10,
      "current": 1,
      "pages": 5
    }
  }
  ```
- **Note**: Results are sorted by `transaction_date DESC` (newest first). The `category` field is eagerly populated.

---

##### `POST /api/transactions` — Create a transaction

- **Auth required**: Yes
- **Request body** (JSON):
  ```json
  {
    "categoryId": 5,              // Required, Long (must be a valid category)
    "type": 2,                    // Required, Integer: 1=income, 2=expense
    "amount": 35.50,              // Required, BigDecimal (positive value)
    "transactionDate": "2024-03-15",  // Required, String (yyyy-MM-dd)
    "note": "Lunch at restaurant" // Optional, String
  }
  ```
- **Success response (200)**: Returns the created `Transaction` object.
- **Note**: The transaction is automatically scoped to the authenticated user.

---

##### `PUT /api/transactions/{id}` — Update a transaction

- **Auth required**: Yes
- **Path variable**: `id` — transaction ID
- **Request body** (JSON):
  ```json
  {
    "categoryId": 6,
    "amount": 42.00,
    "transactionDate": "2024-03-16",
    "note": "Updated note"
  }
  ```
  All fields are optional — only include what you want to change.
- **Success response (200)**: Returns the updated `Transaction` object.
- **Error**: `403` if trying to edit another user's transaction.

---

##### `DELETE /api/transactions/{id}` — Delete a transaction

- **Auth required**: Yes
- **Path variable**: `id` — transaction ID
- **Success response (200)**:
  ```json
  { "code": 200, "message": "success", "data": null }
  ```
- **Note**: Soft delete — the record still exists in the database with `deleted = 1`.

---

### 4.5 Unified Response Format

Every API response follows this structure (`Result<T>`):

```json
{
  "code": 200,           // Integer — HTTP-like status: 200=success, 400=client error, 500=server error
  "message": "success",  // String — human-readable message
  "data": { ... }        // T — the actual payload (object, array, or null)
}
```

**Success codes**: Always `200` (the HTTP status is also 200).  
**Error codes**: `400` for business errors (bad input, duplicate username), `500` for server errors.

### 4.6 Error Handling

The `GlobalExceptionHandler` (`@RestControllerAdvice`) catches exceptions globally:

| Exception | HTTP Status | `code` in body |
|-----------|-------------|----------------|
| `BusinessException` (custom) | 400 | 400 |
| Validation errors (`MethodArgumentNotValidException`) | 400 | 400 |
| Unhandled exceptions | 500 | 500 |

Example error response:
```json
{
  "code": 400,
  "message": "Username already exists",
  "data": null
}
```

---

## 5. Frontend Usage

> **Current state**: The frontend project scaffold is set up with dependencies installed (`axios`, `echarts`, `element-plus`, `vue`), but the application-specific code (router, stores, API layer, views, components) is **not yet implemented**. Only the default Vite + Vue boilerplate exists (`HelloWorld.vue`, `App.vue`). The architecture described below reflects the **planned structure** as documented in `PROJECT_STRUCTURE.md` and `SCAFFOLDING_GUIDE.md`.

### 5.1 Configuration

The frontend uses a `.env.development` file (to be created) to set the API base URL:

```
VITE_API_BASE_URL=http://localhost:8080
```

The `vite.config.js` already configures a dev server proxy:

```javascript
// vite.config.js
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

This means in development, the frontend dev server (port 5173) proxies `/api/*` requests to the backend (port 8080), avoiding CORS issues during development.

### 5.2 Starting the Frontend

```bash
cd bookkeeping-frontend
npm install        # Install dependencies (already done)
npm run dev        # Start Vite dev server
```

The dev server starts at **http://localhost:5173**.  
Build for production: `npm run build` → outputs to `dist/`.

### 5.3 Frontend Architecture

The frontend follows a **layered architecture**:

```
Layer                 Directory          Responsibility
─────────────────────────────────────────────────────────────
Views (Pages)         src/views/         Page-level components, wire together API calls + child components
Components            src/components/    Reusable, dumb components (receive props, emit events)
API Layer             src/api/           One file per backend resource, all HTTP calls centralized
Stores                src/stores/        Pinia stores for global state (auth, UI)
Router                src/router/        Vue Router with navigation guards for auth
Utils                 src/utils/         Axios instance with interceptors, formatters
```

**Planned directory structure** (to be implemented):

```
src/
├── main.js                  ← App entry: register Vue, Router, Pinia, Element Plus
├── App.vue                  ← Root component, <router-view>
├── router/index.js          ← Routes: /login, /register, /dashboard, /transactions, /categories, /profile
├── stores/
│   ├── user.js              ← Auth state: token, userInfo, login(), logout()
│   └── app.js               ← UI state: sidebar collapsed, etc.
├── api/
│   ├── auth.js              ← login(), register(), logout(), getMe()
│   ├── transaction.js       ← getList(), create(), update(), remove()
│   └── category.js          ← getList(), create(), update(), remove()
├── utils/
│   ├── request.js           ← Axios instance with JWT interceptor + 401 handler
│   └── format.js            ← Currency (¥), date formatters
├── layouts/
│   └── AppLayout.vue        ← Sidebar + Header + <router-view> shell
├── views/
│   ├── auth/LoginPage.vue
│   ├── auth/RegisterPage.vue
│   ├── dashboard/DashboardPage.vue
│   ├── transaction/TransactionListPage.vue
│   ├── category/CategoryPage.vue
│   └── profile/UserProfilePage.vue
└── components/
    ├── charts/BarLineChart.vue
    ├── charts/PieChart.vue
    ├── TransactionFormDialog.vue
    └── SummaryCard.vue
```

### 5.4 Route Structure

| Route | Page | Auth Required | Description |
|-------|------|---------------|-------------|
| `/login` | LoginPage | No | Username + password form |
| `/register` | RegisterPage | No | Registration form |
| `/dashboard` | DashboardPage | **Yes** | Summary cards, trend chart, pie chart |
| `/transactions` | TransactionListPage | **Yes** | Paginated table, filter by type, CRUD via dialog |
| `/categories` | CategoryPage | **Yes** | Manage custom categories |
| `/profile` | UserProfilePage | **Yes** | View/edit user info |

The router includes a **navigation guard** (`router.beforeEach`) that:
1. Checks if the route requires authentication (`meta.requiresAuth`)
2. If yes and no token in Pinia store → redirects to `/login`
3. If no token but navigating to login/register → allows through
4. On first load with a stored token, calls `GET /api/auth/me` to restore user info

### 5.5 API Layer Pattern

All API calls go through a shared Axios instance (`src/utils/request.js`) which provides:

**Request Interceptor**:
- Reads JWT token from Pinia store (or localStorage)
- Attaches `Authorization: Bearer <token>` header to every request

**Response Interceptor**:
- Unwraps the `Result<T>` wrapper — extracts `.data` from the response body
- On `401` status: clears token, redirects to `/login`, shows "Session expired" message
- On other errors: shows error message via `ElMessage`

API modules follow this pattern:

```javascript
// src/api/transaction.js
import request from '@/utils/request'

export const transactionApi = {
  getList: (params) => request.get('/transactions', { params }),
  create:  (data)   => request.post('/transactions', data),
  update:  (id, data) => request.put(`/transactions/${id}`, data),
  remove:  (id)     => request.delete(`/transactions/${id}`),
}
```

### 5.6 Auth Flow (Frontend)

1. **Login/Register**: User submits form → POST to `/api/auth/login` or `/api/auth/register` → receive `{ token, userInfo }` → store in Pinia `useUserStore` + localStorage → redirect to `/dashboard`
2. **Subsequent requests**: Axios interceptor automatically adds `Authorization: Bearer <token>` header
3. **Page refresh**: `useUserStore` checks localStorage for token on initialization, calls `GET /api/auth/me` to restore session
4. **Logout**: Clear Pinia store + localStorage → redirect to `/login`
5. **Expired session**: Backend returns 401 → interceptor catches it → clears auth state → redirects to `/login`

---

## 6. Docker Tutorial (Panduan Lengkap untuk Pemula)

> Tutorial ini ditulis dalam Bahasa Indonesia untuk memudahkan kamu yang baru pertama kali menggunakan Docker.  
> Jika sudah pernah menggunakan Docker, langsung lompat ke bagian [6.7 Perintah Cepat (Quick Reference)](#67-perintah-cepat-quick-reference).

### 6.1 Apa itu Docker dan Kenapa Kita Butuh?

Bayangkan kamu punya tiga komponen yang harus berjalan bersamaan:
1. **MySQL** (database)
2. **Spring Boot** (backend API)
3. **Nginx + Vue 3** (frontend)

Tanpa Docker, kamu harus meng-install Java, Maven, Node.js, MySQL satu per satu di laptop kamu. Konfigurasinya bisa berbeda-beda tergantung sistem operasi, dan sering terjadi error "di laptopku bisa, di laptopmu kok error?"

**Docker menyelesaikan masalah ini.** Docker mengemas setiap komponen ke dalam **container** — semacam "kotak" terisolasi yang sudah berisi semua yang dibutuhkan (OS, library, konfigurasi, kode). Container ini:
- Berjalan sama persis di semua komputer (Mac, Windows, Linux)
- Tidak perlu install Java, Node, MySQL secara manual
- Bisa dijalankan dan dimatikan dengan satu perintah
- Data tetap tersimpan meskipun container dimatikan (pakai **volume**)

**Analoginya:** Docker itu seperti kontainer pengiriman barang. Kamu tidak perlu tahu isinya apa dan bagaimana cara kerjanya — kamu cukup tahu kontainernya bisa dipindahkan kemana saja dan isinya tetap aman.

**Istilah Penting:**
| Istilah | Penjelasan Singkat |
|---------|-------------------|
| **Image** | Cetakan / blueprint untuk membuat container. Seperti `.iso` atau installer. Dibaca sekali, tidak berubah. |
| **Container** | Hasil nyata dari image yang sedang berjalan. Seperti "aplikasi yang sedang dibuka". Bisa banyak container dari satu image. |
| **Docker Compose** | Alat untuk menjalankan banyak container sekaligus dengan satu file konfigurasi (`docker-compose.yml`). |
| **Volume** | Tempat penyimpanan data yang bertahan meskipun container dihapus. Untuk database, supaya data tidak hilang. |
| **Port** | "Pintu" untuk mengakses container dari luar. Misalnya port `8080` di container dipetakan ke `8080` di laptop kamu. |

### 6.2 Install Docker Desktop

#### Step 1: Download Docker Desktop

- **Mac (Apple Silicon M1/M2/M3)**: Download dari https://www.docker.com/products/docker-desktop/ → pilih **Mac with Apple Silicon**
- **Mac (Intel)**: Pilih **Mac with Intel Chip**
- **Windows**: Download dari https://www.docker.com/products/docker-desktop/ → pilih **Windows**

#### Step 2: Install

- **Mac**: Buka file `.dmg` yang didownload, drag icon Docker ke folder Applications. Lalu buka Docker dari Applications.
- **Windows**: Jalankan installer `.exe`, ikuti petunjuk, restart komputer jika diminta.

#### Step 3: Verifikasi Instalasi

Setelah Docker Desktop berjalan (ada icon di menu bar / tray), buka **Terminal** (Mac) atau **Command Prompt / PowerShell** (Windows), lalu ketik:

```bash
docker --version
# Harus muncul: Docker version XX.XX.X, build XXXXXXX

docker compose version
# Harus muncul: Docker Compose version vX.XX.X
```

Jika kedua perintah di atas muncul versi, artinya Docker sudah siap digunakan!

### 6.3 Memahami docker-compose.yml Proyek Ini

Buka file `docker-compose.yml` di root project. File ini mendefinisikan **tiga service** (container) yang akan berjalan:

```
┌─────────────────────────────────────────────────────────────┐
│                    DOCKER COMPOSE                           │
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   │
│  │    db        │   │   backend    │   │   frontend   │   │
│  │   MySQL 8.0  │   │ Spring Boot  │   │    Nginx     │   │
│  │              │   │              │   │   Vue 3 SPA  │   │
│  │  Port: 3306  │   │  Port: 8080  │   │  Port: 3000  │   │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘   │
│         │                  │                  │            │
│         └──────────────────┴──────────────────┘            │
│                    Network: bookkeeping-net                 │
│                    (bisa saling komunikasi)                 │
└─────────────────────────────────────────────────────────────┘
```

Penjelasan setiap bagian:

**Service 1: `db` (MySQL)**
- `image: mysql:8.0` → Download MySQL versi 8.0 otomatis dari Docker Hub
- `container_name: bookkeeping-db` → Nama container biar mudah dikenali
- `environment:` → Konfigurasi variabel:
  - `MYSQL_ROOT_PASSWORD: secretpassword` → Password user root MySQL (ganti kalau mau)
  - `MYSQL_DATABASE: bookkeeping_db` → Nama database yang akan dibuat otomatis
- `ports: "3306:3306"` → Port MySQL bisa diakses dari laptop di port 3306
- `volumes:` → Dua volume:
  1. `mysql_data:/var/lib/mysql` → Data MySQL disimpan permanen (tidak hilang saat container mati)
  2. `./db:/docker-entrypoint-initdb.d` → Script SQL dari folder `db/` otomatis dijalankan saat pertama kali container dibuat (schema + seed)
- `healthcheck:` → Cek apakah MySQL sudah benar-benar siap menerima koneksi. Backend akan menunggu MySQL siap sebelum mulai.

**Service 2: `backend` (Spring Boot)**
- `build:` → Build image dari folder `bookkeeping-backend/` menggunakan `Dockerfile` di dalamnya
- `ports: "8080:8080"` → API bisa diakses di `http://localhost:8080`
- `environment:` → Variabel yang akan dipakai Spring Boot:
  - `DB_URL: jdbc:mysql://db:3306/bookkeeping_db...` → **Perhatikan:** hostname-nya `db` (nama service MySQL), bukan `localhost`. Di dalam Docker network, setiap service bisa dipanggil dengan nama service-nya.
  - `DB_USERNAME: root`
  - `DB_PASSWORD: secretpassword`
  - `JWT_SECRET: ...` → Kunci untuk generate token JWT
- `depends_on: db` → Backend hanya akan start SETELAH MySQL selesai dicek kesehatannya (`condition: service_healthy`)

**Service 3: `frontend` (Nginx + Vue)**
- `build:` → Build image dari folder `bookkeeping-frontend/` menggunakan `Dockerfile`
- `ports: "3000:80"` → Website bisa diakses di `http://localhost:3000` (port 3000 di laptop → port 80 di container Nginx)
- `depends_on: - backend` → Frontend start setelah backend

**Network: `bookkeeping-net`**
- Semua service terhubung dalam satu network yang sama, jadi mereka bisa saling berkomunikasi menggunakan nama service. Misalnya backend bisa connect ke MySQL dengan hostname `db`, dan Nginx bisa proxy API ke `backend:8080`.

### 6.4 Cara Menjalankan Aplikasi dengan Docker

#### Step 1: Pastikan Tidak Ada yang Berjalan di Port 3306, 8080, 3000

Sebelum mulai, pastikan port-port ini tidak ada yang pakai:

```bash
# Cek port 3306 (MySQL)
lsof -i :3306
# Cek port 8080 (Backend)
lsof -i :8080
# Cek port 3000 (Frontend)
lsof -i :3000
```

Kalau ada output, artinya ada aplikasi lain yang pakai port tersebut. Matikan dulu, atau ubah port di `docker-compose.yml`.

> **Khusus Mac**: Jika kamu sebelumnya sudah install MySQL langsung di Mac (bukan via Docker), kemungkinan MySQL sedang berjalan di port 3306. Matikan dulu: `brew services stop mysql` atau dari System Preferences → MySQL → Stop.

#### Step 2: Buka Terminal di Folder Project

```bash
cd /Users/samudera/Bagja/Kuliah/SDUST_6/Practice\ of\ Software\ Development
```

#### Step 3: Jalankan Docker Compose

```bash
docker compose up -d
```

Perintah ini akan:
1. Download image MySQL 8.0 (sekitar 500MB, hanya sekali)
2. Download image Maven + Java (untuk build backend, sekitar 300MB)
3. Download image Node.js (untuk build frontend, sekitar 200MB)
4. Build backend (compile Spring Boot dengan Maven) — sekitar 2-5 menit
5. Build frontend (compile Vue dengan Vite) — sekitar 1-3 menit
6. Jalankan ketiga container

Flag `-d` artinya "detached mode" — berjalan di background, terminal tetap bisa dipakai.

**Pertama kali akan lama** karena download image. Selanjutnya cepat karena image sudah ada di cache.

#### Step 4: Cek Status Container

```bash
docker compose ps
```

Harus muncul tiga container dengan status `Up` atau `healthy`:
```
NAME                      STATUS
bookkeeping-db            healthy
bookkeeping-backend       running
bookkeeping-frontend      running
```

#### Step 5: Cek Log (Jika Ada Error)

```bash
# Lihat log semua service
docker compose logs

# Lihat log backend saja (scroll real-time)
docker compose logs -f backend

# Lihat log MySQL saja
docker compose logs db
```

#### Step 6: Buka Aplikasi

- **Frontend (Website)**: Buka http://localhost:3000
- **Backend (API)**: Buka http://localhost:8080

#### Step 7: Test API dengan curl

```bash
# Registrasi user baru
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"pass123","nickname":"Test User"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"pass123"}'
```

### 6.5 Perintah Sehari-hari yang Perlu Kamu Tahu

```bash
# ===== MENJALANKKAN =====
docker compose up -d          # Start semua service di background
docker compose up             # Start semua service (log tampil di terminal, Ctrl+C untuk stop)

# ===== MENGHENTIKAN =====
docker compose stop           # Stop container (tidak hapus data)
docker compose down           # Stop DAN hapus container (data di volume TETAP aman)
docker compose down -v        # Stop, hapus container, DAN hapus volume (DATA HILANG! Hati-hati!)

# ===== RESTART =====
docker compose restart backend   # Restart hanya service backend
docker compose restart            # Restart semua service

# ===== LIHAT STATUS =====
docker compose ps              # Lihat status container
docker compose logs -f         # Lihat log semua (real-time, Ctrl+C untuk keluar)
docker compose logs backend    # Lihat log backend saja

# ===== REBUILD (setelah ada perubahan kode) =====
docker compose build           # Build ulang image (tanpa start)
docker compose up -d --build   # Build ulang + start

# ===== MASUK KE DALAM CONTAINER =====
docker compose exec db mysql -u root -psecretpassword bookkeeping_db   # Masuk ke MySQL shell
docker compose exec backend sh                                         # Masuk ke shell backend
```

### 6.6 Cara Update Setelah Ada Perubahan Kode

Setiap kali kamu mengubah kode backend atau frontend, kamu perlu rebuild:

```bash
# 1. Stop container (tapi jangan hapus volume database)
docker compose down

# 2. Build ulang dan start
docker compose up -d --build

# Atau langsung rebuild service tertentu saja:
docker compose up -d --build backend    # Hanya rebuild backend
docker compose up -d --build frontend   # Hanya rebuild frontend
```

### 6.7 Perintah Cepat (Quick Reference)

```bash
# START (dari folder project)
docker compose up -d

# STOP (data tetap aman)
docker compose down

# STOP + HAPUS SEMUA DATA (hati-hati!)
docker compose down -v

# LIHAT STATUS
docker compose ps

# LIHAT LOG
docker compose logs -f

# REBUILD SETELAH UBAH KODE
docker compose up -d --build

# MASUK MYSQL SHELL
docker compose exec db mysql -u root -psecretpassword bookkeeping_db

# CEK DATA DI DATABASE
docker compose exec db mysql -u root -psecretpassword -e "USE bookkeeping_db; SELECT * FROM categories;"
docker compose exec db mysql -u root -psecretpassword -e "USE bookkeeping_db; SELECT * FROM users;"
docker compose exec db mysql -u root -psecretpassword -e "USE bookkeeping_db; SELECT * FROM transactions;"
```

### 6.8 Troubleshooting (Masalah Umum dan Solusinya)

| Masalah | Kemungkinan Penyebab | Solusi |
|---------|---------------------|--------|
| **Port 3306/8080/3000 already in use** | Ada aplikasi lain yang pakai port tersebut (seperti MySQL lokal) | Ubah port di `docker-compose.yml` (misal `3307:3306`) atau matikan aplikasi yang konflik |
| **Backend tidak bisa connect ke MySQL** | MySQL belum siap saat backend mencoba connect | Backend akan otomatis restart karena ada `depends_on` + `healthcheck`. Tunggu 1-2 menit. |
| **Backend error "Unknown database"** | Nama database di env var tidak cocok | Pastikan `DB_URL` di docker-compose.yml menggunakan nama database `bookkeeping_db` (sama dengan yang ada di `schema.sql`). Perhatikan: default di `application.yml` adalah `bookkeeping` (tanpa `_db`), tapi Docker menggunakan env var yang sudah benar (`bookkeeping_db`). |
| **Frontend blank page / CORS error** | API tidak terproksi dengan benar | Cek `nginx.conf` — harus ada `proxy_pass http://backend:8080;` di location `/api/`. |
| **"No such file or directory" saat build** | Folder `db/` tidak ditemukan | Pastikan kamu menjalankan `docker compose` dari root folder project (tempat `docker-compose.yml` berada). |
| **Docker Desktop "docker not running"** | Docker Desktop belum dibuka | Buka aplikasi Docker Desktop dan tunggu sampai statusnya "Engine Running" |
| **Download image gagal / lambat** | Koneksi internet bermasalah | Coba lagi, atau gunakan VPN. Image hanya perlu didownload sekali. |
| **Data hilang setelah `docker compose down`** | Menggunakan `-v` flag | **Hindari** `docker compose down -v` kecuali kamu benar-benar ingin menghapus semua data. `docker compose down` tanpa `-v` hanya menghapus container, data di volume tetap aman. |

### 6.9 Tips Tambahan

1. **Gunakan Docker Desktop GUI**: Docker Desktop punya tampilan visual yang bagus. Buka Docker Desktop → tab **Containers** → kamu bisa lihat semua container, log, dan statistik (CPU/RAM usage) secara visual.

2. **Hemat resource**: Kalau sudah selesai, jalankan `docker compose stop` untuk menghentikan container. Container yang idle bisa memakan RAM lumayan banyak.

3. **Clean up sesekali**: Docker menyimpan image, container, dan volume yang sudah tidak terpakai. Untuk membersihkan:
   ```bash
   docker system prune -a    # Hapus SEMUA yang tidak terpakai (hati-hati!)
   ```

4. **Database backup**: Data MySQL disimpan di volume `mysql_data`. Untuk backup:
   ```bash
   docker compose exec db mysqldump -u root -psecretpassword bookkeeping_db > backup.sql
   ```

5. **Password default**: Password MySQL root di docker-compose.yml adalah `secretpassword`. Untuk production, ganti dengan password yang kuat!

---

## 7. Running Tests

### Backend Tests

```bash
cd bookkeeping-backend
./mvnw test
```

The test suite includes:
- `BookkeepingBackendApplicationTests.java` — context load test
- `CategoryTransactionIntegrationTest.java` — integration tests for category & transaction CRUD using an H2 in-memory database

Test configuration is in `src/test/resources/application-test.yml` (uses H2, not MySQL).  
Test schema and reset scripts are in `src/test/resources/sql/`.

---

## Quick Reference — Common cURL Commands

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123","nickname":"Test"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123"}'
```
→ Copy the `token` from the response.

### Get My Info
```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <your-token>"
```

### List Categories (expense only)
```bash
curl "http://localhost:8080/api/categories?type=2" \
  -H "Authorization: Bearer <your-token>"
```

### Create a Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"categoryId":5,"type":2,"amount":35.50,"transactionDate":"2024-03-15","note":"Lunch"}'
```

### List Transactions (paginated, filtered)
```bash
curl "http://localhost:8080/api/transactions?page=1&size=10&type=2&startDate=2024-03-01&endDate=2024-03-31" \
  -H "Authorization: Bearer <your-token>"
```

### Update a Transaction
```bash
curl -X PUT http://localhost:8080/api/transactions/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"amount":42.00,"note":"Updated lunch cost"}'
```

### Delete a Transaction
```bash
curl -X DELETE http://localhost:8080/api/transactions/1 \
  -H "Authorization: Bearer <your-token>"
```

### Create a Custom Category
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"name":"Side Hustle","type":1,"icon":"laptop","color":"#2ECC71"}'
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Backend won't start | Check MySQL is running and credentials in `application.yml` are correct |
| `Access denied for user` | Verify MySQL username/password in `application.yml` |
| `Unknown database 'bookkeeping_db'` | Run `db/schema.sql` first |
| `Table doesn't exist` | Run both `db/schema.sql` and `db/seed.sql` |
| 401 on all endpoints | Token expired or missing — login again to get a fresh token |
| 403 on category update/delete | You're trying to modify a system default category (only custom categories can be edited/deleted) |
| CORS errors in browser | Ensure backend CORS filter allows your frontend origin; use the Vite proxy in development |
| Frontend shows blank page | Run `npm install` in `bookkeeping-frontend/` first |
| Port 8080 already in use | Change `server.port` in `application.yml` or kill the existing process |