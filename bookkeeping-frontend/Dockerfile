# ============================================================
# Bookkeeping Frontend — Multi-stage Docker Build
# Stage 1: Build the static files with Node
# Stage 2: Serve with Nginx Alpine
# ============================================================

# ---------- Stage 1: Build ----------
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files first to cache dependencies
COPY package*.json ./
RUN npm ci --only=production=false 2>&1

# Copy source and build
COPY . .
RUN npm run build

# ---------- Stage 2: Serve ----------
FROM nginx:alpine

# Remove default nginx static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Custom nginx config: SPA fallback + gzip
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]