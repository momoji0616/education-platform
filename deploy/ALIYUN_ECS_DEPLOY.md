# Aliyun ECS deployment

This project can be deployed to a single ECS instance with:

- Nginx serving `frontend/dist`
- Spring Boot on `127.0.0.1:8080`
- FastAPI AI service on `127.0.0.1:8000`
- MySQL and Redis on the same host

Use `deploy/scripts/deploy_ecs.sh` from the repository root.

## Routine deployment

```bash
export DEPLOY_HOST=<ecs-public-ip>
export DEPLOY_SSH_KEY=/path/to/ecs.pem

./deploy/scripts/deploy_ecs.sh
```

By default the script:

- Builds the backend jar with Maven
- Builds the frontend production bundle
- Uploads backend, frontend, and AI service code
- Installs or refreshes systemd and Nginx config
- Restarts only the Spring Boot backend
- Runs lightweight health checks

It does not restart the AI service by default because the local Qdrant
collection can use several GB of memory. When AI code or AI environment values
change, run:

```bash
RESTART_AI=1 ./deploy/scripts/deploy_ecs.sh
```

## First-time environment setup

Remote environment files are not overwritten unless explicitly requested.
For first-time setup, provide secrets through your shell environment:

```bash
export DEPLOY_HOST=<ecs-public-ip>
export DEPLOY_SSH_KEY=/path/to/ecs.pem
export UPDATE_REMOTE_ENV=1

export DB_URL='jdbc:mysql://127.0.0.1:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8'
export DB_USERNAME=edu_app
export DB_PASSWORD='change-me'

export AI_DB_HOST=127.0.0.1
export AI_DB_PORT=3306
export AI_DB_NAME=ry-vue
export AI_DB_USER=edu_app
export AI_DB_PASSWORD='change-me'

export DEEPSEEK_API_KEY='change-me'
export DEEPSEEK_BASE_URL='https://api.deepseek.com'
export DEEPSEEK_MODEL='deepseek-chat'

./deploy/scripts/deploy_ecs.sh
```

Do not commit real API keys, database passwords, or SSH private keys.

## Runtime database import

The runtime SQL dumps are intentionally ignored by git. If you have the
required runtime database directory locally, import it with:

```bash
IMPORT_RUNTIME_DB=1 \
RUNTIME_DB_DIR=/path/to/runtime_database \
DB_ROOT_USER=root \
DB_ROOT_PASSWORD='mysql-root-password-if-needed' \
./deploy/scripts/deploy_ecs.sh
```

Expected files in `RUNTIME_DB_DIR`:

- `ry_vue_required_runtime_data.sql`
- `education_legacy_piclass_required_staging_data.sql`

## AI runtime assets

The script does not overwrite remote `ai_service/qdrant_db` or
`ai_service/data` directories. Keep those runtime assets on the server and
upload them separately when bootstrapping a new ECS instance.

## Useful switches

```bash
BUILD_BACKEND=0       # reuse existing local backend jar
BUILD_FRONTEND=0      # reuse existing local frontend dist
DEPLOY_AI=0           # skip AI service source sync
SYNC_AI_DEPS=1        # run uv sync on the server
RESTART_AI=1          # restart FastAPI service
SETUP_NGINX=0         # skip Nginx config refresh
SETUP_SYSTEMD=0       # skip systemd unit refresh
```
