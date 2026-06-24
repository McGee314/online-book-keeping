# Personal Online Bookkeeping System — Usage Guide

This guide explains how to run and use the current version of the application.

> **Current status:** Backend Day 2 is implemented.
> That means the project currently includes:
> - Spring Boot backend setup
> - User registration
> - User login
> - JWT authentication
> - `GET /api/auth/me` for current user info

Frontend pages and bookkeeping business modules such as categories, transactions, budgets, and charts are planned for the next sprint days.

---

## 1. Prerequisites

Make sure these are installed on your machine:

```bash
java -version
node -v
npm -v
mysql --version
```

Recommended versions:
- **Java:** 11
- **Node.js:** 18+
- **MySQL:** 5.7 or 8.0

---

## 2. Current Project Structure

```text
Practice of Software Development/
├── bookkeeping-backend/
├── db/
│   ├── schema.sql
│   └── seed.sql
├── README.md
├── SCAFFOLDING_GUIDE.md
├── PROJECT_STRUCTURE.md
├── SPRINT_PLAN.md
└── USAGE_GUIDE.md
```

---

## 3. Set Up the Database

Create the database tables first:

```bash
mysql -u root -p < db/schema.sql
```

Optional: insert default categories seed data:

```bash
mysql -u root -p < db/seed.sql
```

### Database name
The SQL script creates and uses this database:

```sql
bookkeeping
```

---

## 4. Configure Environment Variables

The backend is configured to read database and JWT settings from environment variables.

### Quick temporary setup in terminal

Run these before starting the backend:

```bash
export DB_URL='jdbc:mysql://localhost:3306/bookkeeping?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false'
export DB_USERNAME='root'
export DB_PASSWORD='your_mysql_password'
export JWT_SECRET='YourSuperSecretKeyThatIsAtLeast256BitsLong!BookkeepingApp2026'
export JWT_EXPIRATION='86400000'
```

### What each variable means

| Variable | Purpose |
|----------|---------|
| `DB_URL` | MySQL JDBC connection string |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Secret key used to sign JWT tokens |
| `JWT_EXPIRATION` | Token expiration time in milliseconds |

---

## 5. Start the Backend

Go to the backend folder:

```bash
cd bookkeeping-backend
```

Use the Maven wrapper to start the server:

```bash
./mvnw spring-boot:run
```

If everything is correct, the backend will start on:

```text
http://localhost:8080
```

---

## 6. Available API Endpoints

The currently available auth endpoints are:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |
| GET | `/api/auth/me` | Get current logged-in user info |

---

## 7. How to Register a User

Use `curl`, Postman, or Apifox.

### Request

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "samudera",
    "password": "secret123",
    "nickname": "Samudera",
    "email": "samudera@example.com"
  }'
```

### Expected success response

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "your_jwt_token_here",
    "userInfo": {
      "id": 1,
      "username": "samudera",
      "nickname": "Samudera",
      "email": "samudera@example.com",
      "avatar": null
    }
  }
}
```

### Validation rules

- `username`: required, 4–20 characters
- `password`: required, 6–20 characters
- `nickname`: required, max 50 characters
- `email`: optional, but must be valid if provided

---

## 8. How to Login

### Request

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "samudera",
    "password": "secret123"
  }'
```

### Expected response

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "your_jwt_token_here",
    "userInfo": {
      "id": 1,
      "username": "samudera",
      "nickname": "Samudera",
      "email": "samudera@example.com",
      "avatar": null
    }
  }
}
```

Save the returned token because you need it for protected APIs.

---

## 9. How to Access a Protected Endpoint

Use the JWT token in the `Authorization` header.

### Request

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer your_jwt_token_here"
```

### Expected response

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "samudera",
    "nickname": "Samudera",
    "email": "samudera@example.com",
    "avatar": null
  }
}
```

---

## 10. Common Error Responses

### Missing token

```json
{
  "code": 401,
  "message": "Missing or invalid Authorization header",
  "data": null
}
```

### Expired or invalid token

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

### Duplicate username/email

```json
{
  "code": 409,
  "message": "Username already exists",
  "data": null
}
```

or

```json
{
  "code": 409,
  "message": "Email already exists",
  "data": null
}
```

### Validation failure

```json
{
  "code": 400,
  "message": "username must not be blank",
  "data": null
}
```

---

## 11. How Authentication Works Internally

The backend auth flow works like this:

1. User registers with username/password.
2. Password is encrypted using **BCrypt**.
3. On login, backend verifies the password.
4. If valid, backend creates a **JWT token**.
5. Client sends that token in:

```http
Authorization: Bearer <token>
```

6. `JwtAuthInterceptor` validates the token on protected routes.
7. The current `userId` is stored in `UserContext` for request-scoped access.

---

## 12. Useful Commands

### Build and run tests

```bash
cd bookkeeping-backend
./mvnw test
```

### Run the application

```bash
cd bookkeeping-backend
./mvnw spring-boot:run
```

### Check git status before push

```bash
git status
```

---

## 13. What Is Not Built Yet

The following features are planned next but are not available yet:

- Category CRUD
- Transaction CRUD
- Dashboard statistics
- Budget module
- Vue frontend pages
- ECharts visualizations

These will be implemented in the next sprint days.

---

## 14. Recommended Next Step

After verifying auth works, continue with:

### Day 3 Backend
- Category module
- Transaction module
- Protected CRUD APIs

Then continue with frontend integration.

---

## 15. Quick Demo Flow

If you want to demonstrate the current system to your lecturer:

1. Start MySQL
2. Import `db/schema.sql`
3. Start backend with `./mvnw spring-boot:run`
4. Register a user using `/api/auth/register`
5. Login using `/api/auth/login`
6. Call `/api/auth/me` using the JWT token

That proves:
- database connection works
- backend boots successfully
- user registration works
- password hashing works
- login works
- JWT authentication works
