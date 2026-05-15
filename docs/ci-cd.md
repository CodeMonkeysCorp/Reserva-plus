# CI/CD

Este projeto usa uma esteira baseada em Docker:

1. valida backend e frontend
2. constroi imagens Docker
3. publica imagens no GitHub Container Registry
4. opcionalmente aciona deploy em um servidor via SSH

## CI

Arquivo:

- `.github/workflows/ci.yml`

Executa em `pull_request` e em push para `main` ou `develop`.

Etapas:

- `mvn -B test`
- `mvn -B -DskipTests package`
- `npm ci`
- `npx tsc --noEmit --project tsconfig.app.json --noUnusedLocals --noUnusedParameters`
- `npm run build`
- build das imagens Docker sem publicar

## CD

Arquivo:

- `.github/workflows/cd.yml`

Executa em push para `main`, tags `v*` e manualmente por `workflow_dispatch`.

Imagens publicadas:

- `ghcr.io/<owner>/<repo>/backend:<branch-ou-tag>`
- `ghcr.io/<owner>/<repo>/frontend:<branch-ou-tag>`
- `ghcr.io/<owner>/<repo>/backend:latest`
- `ghcr.io/<owner>/<repo>/frontend:latest`

## Deploy via GitHub Actions

Para habilitar o deploy automatico, configure no GitHub:

Variables:

- `DEPLOY_ENABLED=true`
- `DEPLOY_PATH=/caminho/no/servidor/reserva-plus`
- `DEPLOY_ENV_FILE=.env.prod` opcional

Secrets:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_PORT` opcional
- `GHCR_USER` opcional, necessario quando as imagens forem privadas
- `GHCR_TOKEN` opcional, necessario quando as imagens forem privadas

No servidor, mantenha:

- `compose.prod.yaml`
- `.env.prod`

Use `.env.prod.example` como ponto de partida e troque senhas, segredo JWT e nomes de imagem.

Tambem existe um gerador para criar o arquivo com segredos fortes:

```powershell
.\scripts\init-prod-env.ps1
```

Linux:

```sh
sh scripts/init-prod-env.sh
```

O job de deploy entra no diretorio `DEPLOY_PATH`, define `BACKEND_IMAGE` e `FRONTEND_IMAGE`, faz `docker compose pull` e sobe a stack.

## Deploy manual

Linux:

```sh
sh scripts/deploy-prod.sh
```

Windows/PowerShell:

```powershell
.\scripts\deploy-prod.ps1
```

Por padrao os scripts usam:

- compose: `compose.prod.yaml`
- env: `.env.prod`

## Exemplo de `.env.prod`

Existe um arquivo versionado de exemplo na raiz:

- `.env.prod.example`

```env
FRONTEND_PORT=80
MYSQL_DATABASE=reserva_plus
MYSQL_USER=reserva_app
MYSQL_PASSWORD=troque-esta-senha
MYSQL_ROOT_PASSWORD=troque-esta-senha-root

BACKEND_IMAGE=ghcr.io/seu-usuario/seu-repo/backend:latest
FRONTEND_IMAGE=ghcr.io/seu-usuario/seu-repo/frontend:latest

JWT_SECRET=troque-por-uma-chave-com-pelo-menos-32-caracteres
JWT_EXPIRATION_MS=86400000
APP_ADMIN_NAME=Administrador
APP_ADMIN_EMAIL=admin@reserva.local
APP_ADMIN_PASSWORD=troque-esta-senha

APP_STORAGE_R2_ENABLED=false
APP_STORAGE_R2_ACCOUNT_ID=
APP_STORAGE_R2_ENDPOINT=
APP_STORAGE_R2_BUCKET=
APP_STORAGE_R2_ACCESS_KEY_ID=
APP_STORAGE_R2_SECRET_ACCESS_KEY=
APP_STORAGE_R2_PUBLIC_BASE_URL=
APP_STORAGE_R2_OBJECT_KEY_PREFIX=reserva-plus
APP_STORAGE_R2_MAX_FILE_SIZE=10MB
```

Nao versione `.env.prod`.
