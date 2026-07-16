# Docker Setup Guide — Run with One Command

> Requires **Docker Desktop** installed.  
> Runs everything in containers — no Java, MySQL, or Node.js needed on your machine.

---

## 1. Install Docker Desktop

Download and install from: **https://www.docker.com/products/docker-desktop/**

Verify:

```powershell
docker --version
docker compose version
```

---

## 2. Clone the Repo

```powershell
git clone https://github.com/McGee314/online-book-keeping.git
cd online-book-keeping
```

---

## 3. Start Everything

```powershell
docker compose up -d
```

> First run builds images and downloads dependencies — takes ~5-10 minutes.

---

## 4. Verify

Check if all 3 containers are running:

```powershell
docker compose ps
```

Should show all status as `Up`:

| Container | URL |
|-----------|-----|
| `bookkeeping-db` | localhost:3306 (internal) |
| `bookkeeping-backend` | http://localhost:8080 |
| `bookkeeping-frontend` | **http://localhost:3000** |

---

## 5. Open the App

Go to **http://localhost:3000** → Register → Login → Done.

The database is automatically created and seeded on first start.

---

## Useful Commands

| Command | What it does |
|---------|-------------|
| `docker compose up -d` | Start all services in background |
| `docker compose ps` | Check running containers |
| `docker compose logs -f` | View all logs (live) |
| `docker compose logs backend` | View backend logs only |
| `docker compose down` | Stop all containers |
| `docker compose down -v` | Stop + delete database data (fresh start) |
| `docker compose build --no-cache` | Rebuild images from scratch |
| `docker compose restart` | Restart all services |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Port already in use | Stop local MySQL/Java on ports 3306, 8080, or 3000 |
| Can't connect to database | Wait 40s — MySQL health check must pass before backend starts |
| Frontend can't reach API | Check `docker compose logs backend` for errors |
| Changes not reflected | Run `docker compose build --no-cache && docker compose up -d` |
| Reset everything | `docker compose down -v && docker compose up -d --build` |