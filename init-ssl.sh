#!/usr/bin/env bash
# ============================================================
# init-ssl.sh — First-time Let's Encrypt certificate issuance
# ============================================================
# Run this script ONCE on your production server to obtain
# the initial SSL certificate for iscash.msamudera.dev.
#
# Prerequisites:
# 1. DNS A/AAAA record for iscash.msamudera.dev → server IP
# 2. Port 80 is open on firewall/security group
# 3. docker compose is already up and running
# ============================================================

set -e

DOMAIN="iscash.msamudera.dev"
EMAIL="your-email@example.com"   # ← CHANGE THIS to your real email

echo "=============================================="
echo "  Let's Encrypt SSL Certificate Init Script"
echo "  Domain: $DOMAIN"
echo "=============================================="
echo ""

# Step 1: Ensure docker compose is running
echo ">>> Checking if frontend container is running..."
if ! docker ps --format '{{.Names}}' | grep -q "bookkeeping-frontend"; then
    echo "ERROR: bookkeeping-frontend container is not running."
    echo "Please start it first: docker compose up -d"
    exit 1
fi

# Step 2: Create required directories
echo ">>> Creating certbot directories..."
mkdir -p ./certbot/conf ./certbot/www

# Step 3: Test ACME challenge endpoint is reachable
echo ">>> Testing ACME challenge endpoint..."
curl -s -o /dev/null -w "%{http_code}" "http://$DOMAIN/.well-known/acme-challenge/test" || true
echo " (ignore 404 — just checking port 80 is open)"

# Step 4: Request certificate using standalone certbot
# We stop the frontend briefly because certbot needs port 80 for standalone mode.
# Alternatively, use --webroot (keeps nginx running):
echo ">>> Requesting Let's Encrypt certificate via webroot plugin..."
docker compose run --rm --entrypoint certbot certbot \
    certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    --force-renewal \
    -d "$DOMAIN"

echo ""
echo ">>> Certificate obtained successfully!"

# Step 5: Restart frontend to pick up the real certificates
echo ">>> Restarting frontend container to use new certificates..."
docker compose restart frontend

echo ""
echo "=============================================="
echo "  SSL Setup Complete!"
echo "  Your site is now accessible at:"
echo "  https://$DOMAIN"
echo ""
echo "  Certificates auto-renew every 12 hours."
echo "  To test manual renewal:"
echo "    docker compose run --rm --entrypoint certbot certbot renew"
echo "=============================================="