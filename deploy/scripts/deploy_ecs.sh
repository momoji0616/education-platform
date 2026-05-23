#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

DEPLOY_HOST="${DEPLOY_HOST:-${SERVER_HOST:-}}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_SSH_KEY="${DEPLOY_SSH_KEY:-${SSH_KEY:-}}"
REMOTE_DIR="${REMOTE_DIR:-/opt/education-platform}"
REMOTE_ENV_DIR="${REMOTE_ENV_DIR:-/etc/education-platform}"
REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"

BUILD_BACKEND="${BUILD_BACKEND:-1}"
BUILD_FRONTEND="${BUILD_FRONTEND:-1}"
DEPLOY_BACKEND="${DEPLOY_BACKEND:-1}"
DEPLOY_FRONTEND="${DEPLOY_FRONTEND:-1}"
DEPLOY_AI="${DEPLOY_AI:-1}"
SYNC_AI_DEPS="${SYNC_AI_DEPS:-0}"
RESTART_BACKEND="${RESTART_BACKEND:-1}"
RESTART_AI="${RESTART_AI:-0}"
SETUP_SYSTEMD="${SETUP_SYSTEMD:-1}"
SETUP_NGINX="${SETUP_NGINX:-1}"
UPDATE_REMOTE_ENV="${UPDATE_REMOTE_ENV:-0}"
IMPORT_RUNTIME_DB="${IMPORT_RUNTIME_DB:-0}"
DB_ROOT_USER="${DB_ROOT_USER:-root}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-}"

BACKEND_JAR="$ROOT_DIR/backend/ruoyi-admin/target/ruoyi-admin.jar"
FRONTEND_DIST="$ROOT_DIR/frontend/dist"
RUNTIME_DB_DIR="${RUNTIME_DB_DIR:-$ROOT_DIR/deploy/runtime_database}"

DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8}"
DB_USERNAME="${DB_USERNAME:-edu_app}"
DB_PASSWORD="${DB_PASSWORD:-}"
EDUCATION_AI_BASE_URL="${EDUCATION_AI_BASE_URL:-http://127.0.0.1:8000}"

AI_DB_HOST="${AI_DB_HOST:-127.0.0.1}"
AI_DB_PORT="${AI_DB_PORT:-3306}"
AI_DB_NAME="${AI_DB_NAME:-ry-vue}"
AI_DB_USER="${AI_DB_USER:-$DB_USERNAME}"
AI_DB_PASSWORD="${AI_DB_PASSWORD:-$DB_PASSWORD}"
DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}"
DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
DEEPSEEK_MODEL="${DEEPSEEK_MODEL:-deepseek-chat}"
DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY:-}"
QWEN_API_KEY="${QWEN_API_KEY:-}"

usage() {
  cat <<'USAGE'
Usage:
  DEPLOY_HOST=<ecs-public-ip> DEPLOY_SSH_KEY=/path/to/key.pem ./deploy/scripts/deploy_ecs.sh

Required:
  DEPLOY_HOST             ECS public IP or DNS name
  DEPLOY_SSH_KEY          SSH private key path

Common options:
  DEPLOY_USER=root
  REMOTE_DIR=/opt/education-platform
  UPDATE_REMOTE_ENV=1     Write /etc/education-platform/*.env from current env
  SETUP_SYSTEMD=1         Install/update systemd unit files
  SETUP_NGINX=1           Install/update nginx reverse proxy config
  RESTART_AI=1            Restart FastAPI AI service after syncing code
  SYNC_AI_DEPS=1          Run uv sync on the server

Database/runtime import options:
  IMPORT_RUNTIME_DB=1
  RUNTIME_DB_DIR=/path/to/runtime_database
  DB_ROOT_USER=root
  DB_ROOT_PASSWORD=...

Sensitive values are read from environment variables and are not stored in git.
USAGE
}

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ -z "$DEPLOY_HOST" || -z "$DEPLOY_SSH_KEY" ]]; then
  usage >&2
  exit 1
fi

require ssh
require rsync

SSH_OPTS=(
  -i "$DEPLOY_SSH_KEY"
  -o StrictHostKeyChecking=accept-new
  -o ServerAliveInterval=20
  -o ServerAliveCountMax=3
)

remote() {
  ssh "${SSH_OPTS[@]}" "$REMOTE" "$@"
}

remote_bash() {
  ssh "${SSH_OPTS[@]}" "$REMOTE" "bash -s" "$@"
}

rsync_to_remote() {
  rsync -az "$@" -e "ssh ${SSH_OPTS[*]}"
}

log() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

backend_env() {
  cat <<EOF
DB_URL=$DB_URL
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
EDUCATION_AI_BASE_URL=$EDUCATION_AI_BASE_URL
EOF
}

ai_env() {
  cat <<EOF
AI_DB_HOST=$AI_DB_HOST
AI_DB_PORT=$AI_DB_PORT
AI_DB_NAME=$AI_DB_NAME
AI_DB_USER=$AI_DB_USER
AI_DB_PASSWORD=$AI_DB_PASSWORD
DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY
DEEPSEEK_BASE_URL=$DEEPSEEK_BASE_URL
DEEPSEEK_MODEL=$DEEPSEEK_MODEL
DASHSCOPE_API_KEY=$DASHSCOPE_API_KEY
QWEN_API_KEY=$QWEN_API_KEY
EOF
}

log "Checking SSH connection to $REMOTE"
remote "mkdir -p '$REMOTE_DIR' '$REMOTE_ENV_DIR'"

if [[ "$BUILD_BACKEND" == "1" ]]; then
  require mvn
  log "Building Spring Boot backend"
  (cd "$ROOT_DIR/backend" && mvn -pl ruoyi-admin -am clean package -DskipTests)
fi

if [[ "$BUILD_FRONTEND" == "1" ]]; then
  require npm
  log "Building frontend"
  (
    cd "$ROOT_DIR/frontend"
    if [[ ! -d node_modules || "${INSTALL_FRONTEND_DEPS:-0}" == "1" ]]; then
      npm ci
    fi
    npm run build:prod
  )
fi

if [[ "$SETUP_SYSTEMD" == "1" ]]; then
  log "Installing systemd units"
  remote_bash <<EOF
set -Eeuo pipefail
cat >/etc/systemd/system/education-ai.service <<'UNIT'
[Unit]
Description=Education Platform FastAPI AI Service
After=network.target mysqld.service

[Service]
Type=simple
WorkingDirectory=$REMOTE_DIR/ai_service
EnvironmentFile=-$REMOTE_ENV_DIR/ai.env
ExecStart=/bin/bash -lc 'uv run uvicorn main:app --host 127.0.0.1 --port 8000'
Restart=always
RestartSec=5
TimeoutStartSec=240

[Install]
WantedBy=multi-user.target
UNIT

cat >/etc/systemd/system/education-backend.service <<'UNIT'
[Unit]
Description=Education Platform Spring Boot Backend
After=network.target mysqld.service redis.service
Wants=education-ai.service

[Service]
Type=simple
WorkingDirectory=$REMOTE_DIR
EnvironmentFile=-$REMOTE_ENV_DIR/backend.env
ExecStart=/usr/bin/java -jar $REMOTE_DIR/backend/ruoyi-admin/target/ruoyi-admin.jar
Restart=always
RestartSec=5
TimeoutStartSec=180

[Install]
WantedBy=multi-user.target
UNIT

systemctl daemon-reload
systemctl enable education-ai education-backend >/dev/null
EOF
fi

if [[ "$SETUP_NGINX" == "1" ]]; then
  log "Installing nginx config"
  remote_bash <<EOF
set -Eeuo pipefail
cat >/etc/nginx/conf.d/education-platform.conf <<'NGINX'
server {
    listen 80;
    server_name _;

    root $REMOTE_DIR/frontend/dist;
    index index.html;

    client_max_body_size 100m;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /prod-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /dev-api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /rag-api/ {
        proxy_pass http://127.0.0.1:8000/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX
nginx -t
systemctl enable nginx >/dev/null
systemctl reload nginx || systemctl restart nginx
EOF
fi

if [[ "$UPDATE_REMOTE_ENV" == "1" ]]; then
  log "Writing remote environment files"
  backend_env | remote "umask 077; cat > '$REMOTE_ENV_DIR/backend.env'"
  ai_env | remote "umask 077; cat > '$REMOTE_ENV_DIR/ai.env'"
fi

if [[ "$DEPLOY_BACKEND" == "1" ]]; then
  if [[ ! -f "$BACKEND_JAR" ]]; then
    echo "Backend jar not found: $BACKEND_JAR" >&2
    exit 1
  fi
  log "Uploading backend jar"
  remote "mkdir -p '$REMOTE_DIR/backend/ruoyi-admin/target'"
  rsync_to_remote "$BACKEND_JAR" "$REMOTE:$REMOTE_DIR/backend/ruoyi-admin/target/ruoyi-admin.jar"
fi

if [[ "$DEPLOY_FRONTEND" == "1" ]]; then
  if [[ ! -d "$FRONTEND_DIST" ]]; then
    echo "Frontend dist not found: $FRONTEND_DIST" >&2
    exit 1
  fi
  log "Uploading frontend dist"
  remote "mkdir -p '$REMOTE_DIR/frontend/dist'"
  rsync_to_remote --delete "$FRONTEND_DIST/" "$REMOTE:$REMOTE_DIR/frontend/dist/"
fi

if [[ "$DEPLOY_AI" == "1" ]]; then
  log "Syncing AI service code"
  remote "mkdir -p '$REMOTE_DIR/ai_service'"
  rsync_to_remote \
    --exclude '.env' \
    --exclude '.venv' \
    --exclude '__pycache__' \
    --exclude 'qdrant_db' \
    --exclude 'chroma_db' \
    --exclude 'data' \
    "$ROOT_DIR/ai_service/" "$REMOTE:$REMOTE_DIR/ai_service/"

  if [[ "$SYNC_AI_DEPS" == "1" ]]; then
    log "Syncing AI Python dependencies"
    remote "cd '$REMOTE_DIR/ai_service' && uv sync --frozen || uv sync"
  fi
fi

if [[ "$IMPORT_RUNTIME_DB" == "1" ]]; then
  if [[ ! -d "$RUNTIME_DB_DIR" ]]; then
    echo "Runtime DB dir not found: $RUNTIME_DB_DIR" >&2
    exit 1
  fi
  log "Uploading and importing runtime database dumps"
  remote "rm -rf /tmp/education-platform-runtime-db && mkdir -p /tmp/education-platform-runtime-db"
  rsync_to_remote "$RUNTIME_DB_DIR/" "$REMOTE:/tmp/education-platform-runtime-db/"
  ssh "${SSH_OPTS[@]}" "$REMOTE" \
    "DB_ROOT_USER=$(printf '%q' "$DB_ROOT_USER") DB_ROOT_PASSWORD=$(printf '%q' "$DB_ROOT_PASSWORD") bash -s" <<'EOF'
set -Eeuo pipefail
MYSQL=(mysql -u"${DB_ROOT_USER:-root}")
if [[ -n "${DB_ROOT_PASSWORD:-}" ]]; then
  MYSQL+=( -p"${DB_ROOT_PASSWORD}" )
fi
"${MYSQL[@]}" -e "CREATE DATABASE IF NOT EXISTS \`ry-vue\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
if [[ -f /tmp/education-platform-runtime-db/education_platform_ry_vue_single_database.sql ]]; then
  "${MYSQL[@]}" ry-vue < /tmp/education-platform-runtime-db/education_platform_ry_vue_single_database.sql
elif [[ -f /tmp/education-platform-runtime-db/ry_vue_required_runtime_data.sql ]]; then
  "${MYSQL[@]}" ry-vue < /tmp/education-platform-runtime-db/ry_vue_required_runtime_data.sql
else
  echo "No ry-vue runtime SQL dump found in /tmp/education-platform-runtime-db" >&2
  exit 1
fi
EOF
fi

if [[ "$RESTART_AI" == "1" ]]; then
  log "Restarting AI service"
  remote "systemctl restart education-ai"
fi

if [[ "$RESTART_BACKEND" == "1" ]]; then
  log "Restarting backend service"
  remote "systemctl restart education-backend"
fi

log "Health checks"
remote_bash <<'EOF'
set -Eeuo pipefail
systemctl is-active education-backend
systemctl is-active nginx
if systemctl is-active --quiet education-ai; then
  systemctl is-active education-ai
fi
curl -fsS -o /dev/null http://127.0.0.1:8080/captchaImage
curl -fsS -o /dev/null http://127.0.0.1:80/
if systemctl is-active --quiet education-ai; then
  curl -fsS -o /dev/null http://127.0.0.1:8000/docs
fi
free -h
EOF

log "Deployment finished: http://$DEPLOY_HOST/"
