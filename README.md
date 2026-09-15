# Nexo Capital

Aplicação full stack para gestão de corretoras, ações e operações de compra e venda, com consolidação da carteira em reais e preservação da moeda local no histórico.

## Tecnologias

- Angular 19 e Nginx
- Java 17 e Spring Boot
- PostgreSQL 17 e Liquibase
- Docker Compose

## Início rápido com Docker

Requisito: Docker Desktop com Docker Compose v2.

```powershell
git clone https://github.com/GestaoDeAcaoFinal/nexo-capital.git
cd nexo-capital
Copy-Item .env.example .env
docker compose config --quiet
docker compose up -d --build --wait
docker compose ps
```

No Linux ou macOS, substitua `Copy-Item` por `cp`.

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- API pelo proxy: http://localhost:4200/api/operacoes/carteira

Para acompanhar os logs:

```shell
docker compose logs -f
```

Para encerrar preservando os dados:

```shell
docker compose down
```

Use `docker compose down --volumes` somente quando quiser apagar o banco local.

## Variáveis de ambiente

O arquivo `.env` não é versionado. Os valores padrão permitem iniciar o ambiente local. Para consultar ativos dos Estados Unidos, configure uma chave válida em `TWELVE_DATA_API_KEY`.

## Testes

Backend:

```powershell
cd backend
./mvnw test
```

No Windows, também é possível usar `mvnw.cmd test`.

Frontend:

```shell
cd frontend
npm ci
npm test -- --watch=false --browsers=ChromeHeadless --progress=false
npm run build
```

O workflow de CI executa os testes e o build automaticamente em pushes e Pull Requests.

## Estrutura

```text
backend/      API Spring Boot, persistência e integrações externas
frontend/     interface Angular e proxy Nginx para a API
compose.yaml  ambiente full stack reproduzível
```
