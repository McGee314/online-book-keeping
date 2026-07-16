# Windows Setup Guide — Run Locally from Scratch

> For someone on Windows who only has **Chocolatey** and **npm**.  
> This guide gets the **frontend + backend + database** running from zero.

---

## 1. Install Missing Tools (PowerShell as Admin)

```powershell
choco install git -y
choco install openjdk11 -y
choco install mysql -y
```

Restart your terminal after install, then verify:

```powershell
java -version
git --version
mysql --version
```

---

## 2. Clone the Repo

```powershell
git clone https://github.com/McGee314/online-book-keeping.git
cd online-book-keeping
```

---

## 3. Setup MySQL

### 3.1 Start MySQL Service

```powershell
Start-Service mysql*
```

### 3.2 Set root Password

```powershell
mysql -u root
```

Then inside MySQL shell:

```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'secretpassword';
FLUSH PRIVILEGES;
EXIT;
```

### 3.3 Import Database

```powershell
mysql -u root -psecretpassword < db\schema.sql
mysql -u root -psecretpassword < db\seed.sql
```

### 3.4 Verify

```powershell
mysql -u root -psecretpassword -e "USE bookkeeping_db; SHOW TABLES;"
```

You should see 4 tables: `budgets`, `categories`, `transactions`, `users`.

---

## 4. Run Backend

```powershell
cd bookkeeping-backend
.\mvnw.cmd spring-boot:run
```

Wait until you see: `Started BookkeepingBackendApplication ...`

Backend runs on: **http://localhost:8080**

> First run downloads dependencies — takes a few minutes.

---

## 5. Run Frontend

Open a **new terminal**, then:

```powershell
cd bookkeeping-frontend
npm install
npm run dev
```

Frontend runs on: **http://localhost:5173**

---

## 6. Open the App

Go to **http://localhost:5173** → Register a new account → Login → Done.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `java` not recognized | Restart terminal or add `C:\Program Files\OpenJDK\openjdk-11.x.x\bin` to PATH |
| MySQL Access Denied | Login with `mysql -u root` (no password) and set it like step 3.2 |
| Port 8080/5173 in use | `netstat -ano \| findstr :8080` then `taskkill /PID <PID> /F` |
| Backend download stuck | `Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository"` then retry |
| Frontend blank | Make sure backend is running first on `localhost:8080` |