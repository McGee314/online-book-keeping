# ============================================================
# Bookkeeping Frontend — Multi-stage Docker Build
# Stage 1: Build the static files with Node
# Stage 2: Serve with Nginx Alpine + SSL startup script
# ============================================================

# ---------- Stage 1: Build ----------
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first to cache dependencies
COPY package*.json ./
RUN npm ci --only=production=false 2>&1

# Copy source and build
COPY . .
RUN npm run build

# ---------- Stage 2: Serve ----------
FROM nginx:alpine

# Install openssl for self-signed fallback SSL certificate generation
RUN apk add --no-cache openssl

# Remove default nginx static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# ---------- SSL bootstrap script ----------
# This script generates self-signed fallback certs on first run
# so nginx can start even before Let's Encrypt certs exist.
# Once certbot obtains real certs, nginx will use those on restart.
COPY <<"EOF" /docker-entrypoint.d/40-generate-fallback-certs.sh
#!/bin/sh
set -e

CERT_DIR="/etc/letsencrypt/live/iscash.msamudera.dev"
FULLCHAIN="$CERT_DIR/fullchain.pem"
PRIVKEY="$CERT_DIR/privkey.pem"

if [ ! -f "$FULLCHAIN" ] || [ ! -f "$PRIVKEY" ]; then
    echo "=== No Let's Encrypt certificates found, generating self-signed fallback ==="
    mkdir -p "$CERT_DIR"
    openssl req -x509 -nodes -days 30 \
        -newkey rsa:2048 \
        -keyout "$PRIVKEY" \
        -out "$FULLCHAIN" \
        -subj "/CN=iscash.msamudera.dev" \
        -addext "subjectAltName=DNS:iscash.msamudera.dev" 2>/dev/null
    echo "=== Self-signed fallback certificates created (valid 30 days) ==="
else
    echo "=== Let's Encrypt certificates found, using those ==="
fi
EOF

RUN chmod +x /docker-entrypoint.d/40-generate-fallback-certs.sh

EXPOSE 80 443

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]