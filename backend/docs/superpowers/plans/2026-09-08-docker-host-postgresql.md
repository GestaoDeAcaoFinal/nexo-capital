# Docker com PostgreSQL local Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Executar frontend e backend em contêineres separados, conectando o backend ao database `gestaodeacao` existente no PostgreSQL 16 do Windows sem copiar, substituir ou apagar seus dados.

**Architecture:** O Nginx do frontend serve o build Angular e encaminha `/api/*` ao backend pela rede do Compose. O backend usa `host.docker.internal:5432` para acessar o PostgreSQL do Windows e não inicia um contêiner de banco no perfil `host-postgresql`.

**Tech Stack:** Angular 19, Node 22 Alpine, Nginx Alpine, Java 17, Spring Boot 4.0.6, PostgreSQL 16, Docker Desktop e Docker Compose v2.

**Spec:** `docs/superpowers/specs/2026-09-08-docker-host-postgresql-design.md`

## Global Constraints

- Frontend e backend devem executar em contêineres separados.
- O PostgreSQL 16 instalado no Windows é a fonte única dos dados existentes do database `gestaodeacao`.
- Nenhum volume Docker deve ser montado no diretório de dados do PostgreSQL do Windows.
- Nenhum comando pode excluir, recriar, importar ou substituir o database existente.
- Credenciais reais permanecem apenas no `.env`, que não é versionado.
- A comparação dos dados deve ser somente leitura; changesets pendentes do Liquibase só podem ser aplicados depois da confirmação do backup.
- O perfil Compose novo se chama `host-postgresql`.
- O perfil existente `postgresql`, que usa banco em contêiner, deve continuar disponível.
- O frontend usa `http://localhost:4200` e o backend usa `http://localhost:8080` por padrão.
- Antes da primeira inicialização deve existir backup recente ou autorização explícita para criar um com `pg_dump`.

## File Structure

O diretório do backend é a raiz deste plano. O frontend está no repositório irmão `../gestao-acao-front`.

### Frontend

- `../gestao-acao-front/Dockerfile`: compila o Angular e produz a imagem Nginx de runtime.
- `../gestao-acao-front/.dockerignore`: exclui dependências, build e metadados locais do contexto Docker.
- `../gestao-acao-front/nginx.conf`: entrega a SPA e encaminha `/api/` ao serviço `app-host-postgresql`.
- `../gestao-acao-front/src/environments/environment.production.ts`: define `/api` como base da API no build de produção.

### Backend/orquestração

- `compose.yaml`: adiciona os serviços `app-host-postgresql` e `frontend` no perfil `host-postgresql`.
- `.env.example`: documenta porta do frontend e porta do PostgreSQL do host sem incluir segredos.
- `README.md`: documenta preparação, backup, inicialização, verificação, encerramento e diagnóstico.

---

### Task 1: Imagem de produção do frontend

**Files:**
- Create: `../gestao-acao-front/Dockerfile`
- Create: `../gestao-acao-front/.dockerignore`
- Create: `../gestao-acao-front/nginx.conf`
- Modify: `../gestao-acao-front/src/environments/environment.production.ts`

**Interfaces:**
- Consumes: serviço Compose chamado `app-host-postgresql`, ouvindo na porta interna `8080`.
- Produces: imagem `gestao-acao-front:local`, porta interna `80` e proxy HTTP `/api/* -> http://app-host-postgresql:8080/*`.

- [ ] **Step 1: Confirmar que ainda não existe uma imagem Docker definida**

Run from `../gestao-acao-front`:

```powershell
docker build -t gestao-acao-front:plan-check .
```

Expected: FAIL indicando que não existe `Dockerfile`.

- [ ] **Step 2: Criar o Dockerfile multi-stage**

Create `../gestao-acao-front/Dockerfile`:

```dockerfile
# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/gestao-acao-front/browser/ /usr/share/nginx/html/

EXPOSE 80
```

- [ ] **Step 3: Limitar o contexto enviado ao Docker**

Create `../gestao-acao-front/.dockerignore`:

```dockerignore
.angular
.git
.gitignore
.vscode
coverage
dist
node_modules
*.log
```

- [ ] **Step 4: Configurar a SPA e o proxy reverso**

Create `../gestao-acao-front/nginx.conf`:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://app-host-postgresql:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- [ ] **Step 5: Usar o proxy no ambiente de produção**

Replace `../gestao-acao-front/src/environments/environment.production.ts` with:

```typescript
export const environment = {
  production: true,
  apiBaseUrl: '/api'
} as const;
```

- [ ] **Step 6: Executar testes e build Angular**

Run from `../gestao-acao-front`:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build
```

Expected: todos os testes PASS e arquivos produzidos em `dist/gestao-acao-front/browser`.

- [ ] **Step 7: Construir a imagem do frontend**

Run from `../gestao-acao-front`:

```powershell
docker build -t gestao-acao-front:local .
```

Expected: build concluído e estágio final baseado em Nginx.

- [ ] **Step 8: Commitar somente os arquivos do frontend**

Run from `../gestao-acao-front`:

```powershell
git add -- Dockerfile .dockerignore nginx.conf src/environments/environment.production.ts
git diff --cached --check
git commit -m "build: adicionar container de producao do frontend"
```

Expected: commit no repositório do frontend sem incluir alterações preexistentes não relacionadas.

---

### Task 2: Perfil Compose conectado ao PostgreSQL do Windows

**Files:**
- Modify: `compose.yaml`
- Modify: `.env.example`

**Interfaces:**
- Consumes: imagem `gestao-acao-front:local`, hostname Docker Desktop `host.docker.internal`, database e credenciais do `.env`.
- Produces: perfil `host-postgresql` com serviços `app-host-postgresql` e `frontend`.

- [ ] **Step 1: Criar uma verificação que falha enquanto os serviços não existem**

Run from the backend root:

```powershell
$services = docker compose --profile host-postgresql config --services
if ($LASTEXITCODE -ne 0) { throw 'Compose atual é inválido' }
if ($services -notcontains 'app-host-postgresql') { throw 'Serviço app-host-postgresql ausente' }
if ($services -notcontains 'frontend') { throw 'Serviço frontend ausente' }
```

Expected: FAIL com `Serviço app-host-postgresql ausente`.

- [ ] **Step 2: Adicionar os serviços ao `compose.yaml`**

Insert under `services:` without changing the existing `app-postgresql`, `postgres`, `app-mysql` and `mysql` services:

```yaml
  app-host-postgresql:
    <<: *app-base
    profiles: ["host-postgresql"]
    environment:
      <<: *app-environment
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:postgresql://host.docker.internal:${HOST_POSTGRES_PORT:-5432}/${POSTGRES_DB:-gestaodeacao}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}

  frontend:
    image: gestao-acao-front:local
    build:
      context: ../gestao-acao-front
      dockerfile: Dockerfile
    profiles: ["host-postgresql"]
    ports:
      - "${FRONTEND_PORT:-4200}:80"
    depends_on:
      - app-host-postgresql
```

Do not add a `postgres` dependency to `app-host-postgresql`; its database runs on Windows, outside Compose.

- [ ] **Step 3: Documentar as novas variáveis no exemplo**

Update `.env.example` so its relevant non-secret section is:

```dotenv
# PostgreSQL 16 instalado no Windows e acessado pelos containers.
DB_USERNAME=postgres
DB_PASSWORD=replace_with_your_database_password
POSTGRES_DB=gestaodeacao
HOST_POSTGRES_PORT=5432

APP_PORT=8080
FRONTEND_PORT=4200
```

Keep the existing variables used by the isolated PostgreSQL and MySQL profiles. Do not copy the real `.env` value into this file.

- [ ] **Step 4: Validar interpolação e lista de serviços sem imprimir segredos**

Run from the backend root:

```powershell
docker compose --profile host-postgresql config --quiet
$services = docker compose --profile host-postgresql config --services
if ($services -notcontains 'app-host-postgresql') { throw 'Serviço app-host-postgresql ausente' }
if ($services -notcontains 'frontend') { throw 'Serviço frontend ausente' }
```

Expected: commands exit successfully. Do not run `docker compose config` without `--quiet`, because the expanded output can contain the database password.

- [ ] **Step 5: Executar a suíte do backend**

```powershell
mvn test
```

Expected: BUILD SUCCESS with no failed tests.

- [ ] **Step 6: Commitar somente a orquestração**

```powershell
git add -- compose.yaml .env.example
git diff --cached --check
git commit -m "build: conectar containers ao postgres local"
```

Expected: commit no repositório do backend sem incluir alterações preexistentes não relacionadas.

---

### Task 3: Documentação operacional e proteção dos dados

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: perfil `host-postgresql` e variáveis definidas na Task 2.
- Produces: procedimento reproduzível para backup, inicialização, verificação e encerramento.

- [ ] **Step 1: Verificar que o comando do perfil ainda não está documentado**

```powershell
if (Select-String -LiteralPath README.md -SimpleMatch 'docker compose --profile host-postgresql up -d --build') {
  throw 'A documentação já contém o comando; revise o estado antes de continuar'
}
```

Expected: command exits successfully without output.

- [ ] **Step 2: Adicionar a seção `Docker com PostgreSQL do Windows`**

Add these exact operational points to `README.md`:

````markdown
## Docker com PostgreSQL do Windows

Este perfil inicia dois contêineres: `frontend` e `app-host-postgresql`. O banco não roda em contêiner; o backend acessa o PostgreSQL do Windows por `host.docker.internal`.

Antes da primeira execução, confirme que existe um backup recente do database `gestaodeacao` e preencha `DB_USERNAME` e `DB_PASSWORD` no `.env`. Não versione esse arquivo.

```powershell
docker compose --profile host-postgresql config --quiet
docker compose --profile host-postgresql up -d --build
docker compose --profile host-postgresql ps
```

- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- API pelo proxy: `http://localhost:4200/api/operacoes/carteira`

Para encerrar os contêineres sem tocar no PostgreSQL do Windows:

```powershell
docker compose --profile host-postgresql down
```

Não use `down --volumes` como parte deste procedimento. Se a conexão for recusada, verifique o serviço PostgreSQL, a porta `5432`, o firewall, `listen_addresses` e uma regra restrita no `pg_hba.conf` para a rede do Docker Desktop.
````

Preserve the README sections for H2, isolated PostgreSQL and MySQL.

- [ ] **Step 3: Conferir que todos os comandos e URLs estão presentes**

```powershell
$required = @(
  'docker compose --profile host-postgresql config --quiet',
  'docker compose --profile host-postgresql up -d --build',
  'docker compose --profile host-postgresql down',
  'http://localhost:4200/api/operacoes/carteira',
  'host.docker.internal'
)
$readme = Get-Content -Raw -LiteralPath README.md
foreach ($item in $required) {
  if (-not $readme.Contains($item)) { throw "README não contém: $item" }
}
```

Expected: command exits successfully without output.

- [ ] **Step 4: Commitar somente a documentação**

```powershell
git add -- README.md
git diff --cached --check
git commit -m "docs: explicar deploy com postgres do windows"
```

Expected: documentation commit without unrelated staged files.

---

### Task 4: Preflight, implantação e validação somente leitura

**Files:**
- No source file changes expected.
- Optional backup artifact outside both Git repositories, created only after explicit authorization.

**Interfaces:**
- Consumes: the two images and the `host-postgresql` profile produced by Tasks 1–3.
- Produces: two running application containers that display data from the existing `gestaodeacao` database.

- [ ] **Step 1: Confirmar o backup antes de conectar**

Ask the user to confirm a recent backup. If no backup exists, request explicit authorization before running this non-destructive backup command and let `pg_dump` prompt for the password:

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\Desktop\PostgreSQL-backups"
& 'C:\Program Files\PostgreSQL\16\bin\pg_dump.exe' --host localhost --port 5432 --username postgres --format=custom --file "$env:USERPROFILE\Desktop\PostgreSQL-backups\gestaodeacao-pre-docker.dump" gestaodeacao
```

Expected: `pg_dump` exits with code 0 and the dump file has non-zero length. Never print or store the password in a command, log or tracked file.

- [ ] **Step 2: Confirmar o PostgreSQL e as portas antes de iniciar**

```powershell
Get-Service postgresql-x64-16
netstat -ano | Select-String ':5432'
netstat -ano | Select-String ':8080|:4200'
```

Expected: PostgreSQL status is `Running` and port `5432` is listening. Identify owners of occupied application ports before stopping anything.

- [ ] **Step 3: Preservar volumes ao encerrar uma implantação anterior do mesmo Compose**

After identifying that the listeners belong to this Compose project and receiving approval to replace the running application containers:

```powershell
docker compose down
```

Expected: previous application containers stop. Do not pass `--volumes`, so isolated Docker database volumes remain intact.

- [ ] **Step 4: Validar o Compose sem revelar credenciais**

```powershell
docker compose --profile host-postgresql config --quiet
```

Expected: exit code 0. If variables are missing, update only the ignored `.env` and rerun.

- [ ] **Step 5: Construir e iniciar exatamente os dois serviços**

```powershell
docker compose --profile host-postgresql up -d --build app-host-postgresql frontend
docker compose --profile host-postgresql ps
```

Expected: `app-host-postgresql` and `frontend` are running; no PostgreSQL container is started by this command.

- [ ] **Step 6: Diagnosticar a conexão sem expor a senha**

```powershell
docker compose --profile host-postgresql logs --tail 200 app-host-postgresql
```

Expected: Spring reports successful startup with profile `dev`, Liquibase completes and Hikari connects to PostgreSQL. The log must not contain authentication, `pg_hba.conf`, timeout, schema-validation or migration errors.

If PostgreSQL rejects the Docker address, obtain the source subnet from the PostgreSQL log and add only the narrow required `host ... scram-sha-256` rule to `pg_hba.conf`; reload the PostgreSQL service and retry. Do not add an unrestricted `0.0.0.0/0` rule.

- [ ] **Step 7: Consultar dados existentes diretamente e pelo frontend**

```powershell
$direct = Invoke-RestMethod -Uri 'http://localhost:8080/operacoes/carteira'
$proxied = Invoke-RestMethod -Uri 'http://localhost:4200/api/operacoes/carteira'
$directJson = $direct | ConvertTo-Json -Depth 10 -Compress
$proxiedJson = $proxied | ConvertTo-Json -Depth 10 -Compress
if ($directJson -ne $proxiedJson) { throw 'Proxy e backend retornaram carteiras diferentes' }
$direct | Select-Object quantidadeTotal, precoMedioCarteira, custoTotalCarteira, valorAtualCarteira
```

Expected: both requests return HTTP 200 and identical portfolio summaries. Compare the displayed totals with a known read-only view in pgAdmin; do not insert test rows.

- [ ] **Step 8: Confirmar persistência após reiniciar os contêineres**

```powershell
$before = Invoke-RestMethod -Uri 'http://localhost:4200/api/operacoes/carteira' | ConvertTo-Json -Depth 10 -Compress
docker compose --profile host-postgresql restart app-host-postgresql frontend
$deadline = (Get-Date).AddMinutes(2)
do {
  Start-Sleep -Seconds 2
  try { $afterResponse = Invoke-RestMethod -Uri 'http://localhost:4200/api/operacoes/carteira' } catch { $afterResponse = $null }
} until ($afterResponse -or (Get-Date) -gt $deadline)
if (-not $afterResponse) { throw 'Aplicação não respondeu após o reinício' }
$after = $afterResponse | ConvertTo-Json -Depth 10 -Compress
if ($before -ne $after) { throw 'Resumo mudou após reiniciar os contêineres' }
```

Expected: identical JSON before and after restart, proving that application container lifecycle does not replace the Windows database.

- [ ] **Step 9: Executar a verificação final**

```powershell
mvn test
git diff --check
Push-Location '..\gestao-acao-front'
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build
git diff --check
Pop-Location
docker compose --profile host-postgresql ps
```

Expected: Maven and Angular tests PASS, Angular production build succeeds, diffs contain no whitespace errors, and both application containers remain running.
