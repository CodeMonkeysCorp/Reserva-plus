#!/usr/bin/env sh
set -eu

OUTPUT_PATH="${OUTPUT_PATH:-.env.prod}"
BACKEND_IMAGE="${BACKEND_IMAGE:-reserva-plus-backend:ci}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-reserva-plus-frontend:ci}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@reserva.local}"
ADMIN_NAME="${ADMIN_NAME:-Administrador}"
FORCE="${FORCE:-false}"

if [ -f "$OUTPUT_PATH" ] && [ "$FORCE" != "true" ]; then
  echo "Arquivo $OUTPUT_PATH ja existe. Use FORCE=true para recriar."
  exit 1
fi

new_hex_secret() {
  bytes="${1:-32}"
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "$bytes"
    return
  fi

  od -An -N "$bytes" -tx1 /dev/urandom | tr -d ' \n'
}

MYSQL_PASSWORD="$(new_hex_secret 24)"
MYSQL_ROOT_PASSWORD="$(new_hex_secret 24)"
JWT_SECRET="$(new_hex_secret 48)"
APP_ADMIN_PASSWORD="$(new_hex_secret 18)"

cat > "$OUTPUT_PATH" <<EOF
# Production Compose
FRONTEND_PORT=80
MYSQL_DATABASE=reserva_plus
MYSQL_USER=reserva_app
MYSQL_PASSWORD=$MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD

# Images published by CD
BACKEND_IMAGE=$BACKEND_IMAGE
FRONTEND_IMAGE=$FRONTEND_IMAGE

# Backend / Spring Boot
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION_MS=86400000
APP_ADMIN_NAME=$ADMIN_NAME
APP_ADMIN_EMAIL=$ADMIN_EMAIL
APP_ADMIN_PASSWORD=$APP_ADMIN_PASSWORD
RESERVA_CONCLUSAO_CRON=0 0 * * * *
RESERVA_CONCLUSAO_ZONE=America/Sao_Paulo

# Cloudflare R2
APP_STORAGE_R2_ENABLED=false
APP_STORAGE_R2_ACCOUNT_ID=
APP_STORAGE_R2_ENDPOINT=
APP_STORAGE_R2_BUCKET=
APP_STORAGE_R2_ACCESS_KEY_ID=
APP_STORAGE_R2_SECRET_ACCESS_KEY=
APP_STORAGE_R2_PUBLIC_BASE_URL=
APP_STORAGE_R2_OBJECT_KEY_PREFIX=reserva-plus
APP_STORAGE_R2_MAX_FILE_SIZE=10MB
EOF

echo "Arquivo $OUTPUT_PATH criado com segredos fortes."
echo "Guarde a senha inicial do admin em um cofre antes do primeiro deploy."
