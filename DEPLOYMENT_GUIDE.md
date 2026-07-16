# 🚀 Deployment Guide — Run on a Production Server

This guide covers deploying the **isCash** personal bookkeeping system to a Linux production server using Docker Compose with HTTPS (Let's Encrypt).

---

## 📋 Prerequisites

### Server Requirements

| Requirement | Minimum |
|-------------|---------|
| OS | Ubuntu 20.04+ / Debian 11+ / any Linux with Docker |
| CPU | 1 core |
| RAM | 2 GB |
| Disk | 10 GB free |
| Open Ports | **80** (HTTP), **443** (HTTPS) |

### Software Requirements

Install Docker and Docker Compose:

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose plugin
sudo apt-get update
sudo apt-get install docker-compose-plugin -y

# Log out and back in for group changes to take effect
exit
```

Verify installation:

```bash
docker --version
docker compose version
```

### DNS Setup

**Before starting**, point your domain to the server's IP:

- Create an **A record**: `iscash.msamudera.dev` → `YOUR_SERVER_IP`

Verify DNS is resolving (after propagation):

```bash
ping iscash.msamudera.dev
# Should show your server's IP
```

---

## 📦 Step 1: Clone the Repository

SSH into your server and clone:

```bash
git clone https://github.com/McGee314/online-book-keeping.git
cd online-book-keeping
```

---

## 🐳 Step 2: Start the Application

### First-time deployment

```bash
docker compose up -d --build
```

> **What happens:**
> - MySQL container starts (database auto-created from `db/schema.sql`)
> - Spring Boot backend builds and starts (port 8080)
> - Vue frontend builds and starts with Nginx (ports 80 + 443)
> - Self-signed fallback SSL certs are generated automatically (so nginx doesn't crash)

**This takes 5–10 minutes** on first build (Maven dependencies + npm install + Docker image building).

### Check if everything is running

```bash
docker compose ps
```

All 4 containers should show `Up`:

| Container | Purpose |
|-----------|---------|
| `bookkeeping-db` | MySQL database |
| `bookkeeping-backend` | Spring Boot API |
| `bookkeeping-frontend` | Nginx + Vue SPA |
| `bookkeeping-certbot` | SSL auto-renewal |

### View logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs backend
docker compose logs frontend
```

---

## 🔒 Step 3: Enable HTTPS (Let's Encrypt)

The first run uses self-signed certificates (browser warning). Get a real SSL certificate:

```bash
# 1. Edit the email in init-ssl.sh
nano init-ssl.sh
# Change: EMAIL="your-email@example.com" → your actual email

# 2. Make executable and run
chmod +x init-ssl.sh
./init-ssl.sh
```

**What the script does:**
1. Checks the frontend container is running
2. Runs certbot via Docker to obtain a free Let's Encrypt certificate (webroot method — ACME challenge on port 80)
3. Restarts the frontend container to load the real certificate
4. The certbot container auto-renews every 12 hours in the background

**Verify HTTPS:**

```bash
curl -I https://iscash.msamudera.dev
# Should return HTTP/2 200
```

---

## 🌐 Step 4: Open the App

Visit **https://iscash.msamudera.dev** in your browser.

- HTTP traffic (port 80) is automatically redirected to HTTPS
- Register a new account, log in, and start tracking your finances

---

## 🔧 Useful Management Commands

| Command | Description |
|---------|-------------|
| `docker compose ps` | Show running containers and status |
| `docker compose logs -f` | Follow all logs in real-time |
| `docker compose logs backend` | View backend logs |
| `docker compose logs frontend` | View frontend/nginx logs |
| `docker compose down` | Stop all containers |
| `docker compose up -d` | Start all containers (no rebuild) |
| `docker compose restart` | Restart all services |
| `docker compose restart frontend` | Restart only frontend (after config changes) |
| `docker compose build --no-cache` | Full clean rebuild of images |
| `docker compose run --rm --entrypoint certbot certbot renew` | Manually trigger SSL renewal |
| `docker compose exec db mysql -u root -psecretpassword bookkeeping_db` | Access MySQL directly |

### Git Pull + Redeploy

When you push new code:

```bash
cd online-book-keeping
git pull
docker compose up -d --build
```

### Reset Everything (fresh start)

```bash
docker compose down -v          # Stops + deletes database data
docker compose up -d --build    # Fresh rebuild from scratch
```

> ⚠️ `down -v` **deletes all data** including transactions and users!

---

## 🔒 Security Notes

### Firewall

Ensure only ports 80, 443, and optionally 22 (SSH) are open:

```bash
# UFW example
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

Port 3306 (MySQL) and 8080 (backend) should **NOT** be exposed to the internet. They are only accessible within the Docker network by default in the current `docker-compose.yml`.

If your server is behind a cloud firewall (AWS Security Group, etc.), remove the port 3306 and 8080 rules from `docker-compose.yml` before deploying:

```yaml
# Comment out or remove these lines to block external access:
# ports:
#   - "3306:3306"
#   - "8080:8080"
```

---

## 🗂 File Structure (on Server)

```
online-book-keeping/
├── docker-compose.yml          # Orchestrates all services
├── init-ssl.sh                 # One-time SSL certificate script
├── certbot/                    # Let's Encrypt data (auto-created)
│   ├── conf/                   # Certificates live here
│   └── www/                    # ACME challenge webroot
├── uploads_data/               # Docker volume (uploaded avatars)
├── mysql_data/                 # Docker volume (database files)
├── bookkeeping-backend/        # Spring Boot source
├── bookkeeping-frontend/       # Vue 3 source
└── db/                         # SQL schema + migrations
```

---

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 80/443 already in use | Stop any existing web server (`sudo systemctl stop nginx apache2`) |
| Frontend can't connect to API | Check: `docker compose logs backend` |
| MySQL connection refused | Wait 40s — MySQL health check must pass before backend starts |
| SSL certificate error | Run `./init-ssl.sh` — ensure DNS is pointing to your server |
| Backend build fails | Check Java version: needs Java 17 (`mvn --version`) |
| "No space left on device" | Run `docker system prune -a` to clean old images |
| Changes not reflected | `docker compose build --no-cache && docker compose up -d` |
| Certbot renewal fails | Ensure port 80 is open: `sudo ufw allow 80/tcp` |
| Want to use different domain | Update `nginx.conf` (2 places: `server_name`), `docker-compose.yml` (certbot volumes), `init-ssl.sh` (DOMAIN), and `.env.production` |

---

## 📊 Architecture (Production)

```
                       Internet
                          │
                    ┌─────▼─────┐
                    │  Port 80  │──► HTTP → HTTPS redirect
                    │  Port 443 │──► HTTPS (Let's Encrypt)
                    └─────┬─────┘
                          │
              ┌───────────▼───────────┐
              │   Nginx (frontend)    │
              │   Serves Vue SPA      │
              │   Proxies /api/*      │
              │   Proxies /uploads/*  │
              └───────────┬───────────┘
                          │
              ┌───────────▼───────────┐
              │  Spring Boot (backend)│
              │  REST API :8080       │
              │  JWT Auth             │
              └───────────┬───────────┘
                          │
              ┌───────────▼───────────┐
              │   MySQL (database)    │
              │   Port 3306           │
              └───────────────────────┘