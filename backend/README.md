# Gestão de Ações

API REST em Java 17 e Spring Boot para cadastro de corretoras, ações e operações de compra e venda. O projeto usa Liquibase para versionar o schema, oferece H2 para execução local e pode executar com PostgreSQL ou MySQL por Docker Compose.

## Tecnologias

- Java 17
- Spring Boot 4
- Spring Data JPA e Hibernate
- Liquibase
- H2, PostgreSQL e MySQL
- Maven Wrapper
- Docker e Docker Compose

## Requisitos

Para executar diretamente na máquina:

- JDK 17
- acesso à internet para as integrações externas

Para executar em containers:

- Docker Desktop com Docker Compose v2

O Maven não precisa ser instalado separadamente, pois o projeto inclui o Maven Wrapper.

## Configuração

Copie `.env.example` para `.env` e substitua somente os valores necessários. O arquivo `.env` é ignorado pelo Git e não deve ser versionado.

Variáveis da aplicação:

| Variável | Uso | Padrão |
| --- | --- | --- |
| `TWELVE_DATA_API_KEY` | Chave da Twelve Data para ações dos EUA | sem valor real |
| `DB_URL` | URL JDBC ao executar um banco externo fora do Compose | — |
| `DB_USERNAME` | Usuário do banco externo | — |
| `DB_PASSWORD` | Senha do banco externo | — |
| `DDL_AUTO` | Política de schema do Hibernate | `validate` fora do H2 |
| `HTTP_CLIENT_CONNECT_TIMEOUT` | Timeout de conexão das integrações | `2s` |
| `HTTP_CLIENT_READ_TIMEOUT` | Timeout de leitura das integrações | `5s` |
| `CVM_CACHE_TTL` | Validade do cadastro de intermediários da CVM | `24h` |

O Compose também aceita `APP_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` e `MYSQL_PORT`.

## Perfis de persistência

| Perfil Spring | Banco | Configuração |
| --- | --- | --- |
| padrão / `h2` | H2 em memória | `application-h2.properties` |
| `dev` | PostgreSQL | `application-dev.properties` e `DB_*` |
| `mysql` | MySQL | `application-mysql.properties` e `DB_*` |

O perfil PostgreSQL chama-se `dev` por convenção deste projeto.

## Execução local com H2

No PowerShell, a chave da Twelve Data e opcional para iniciar a aplicaÃ§Ã£o e usar ativos brasileiros. Para consultar ativos dos EUA, defina `TWELVE_DATA_API_KEY` no arquivo `.env` ou no terminal.

```powershell
.\mvnw.cmd spring-boot:run
```

Opcionalmente, para informar a chave apenas na sessÃ£o atual:

```powershell
$env:TWELVE_DATA_API_KEY = "test-key"
.\mvnw.cmd spring-boot:run
```

Nenhum perfil precisa ser informado. A API ficará disponível em `http://localhost:8080` e o console H2 em `http://localhost:8080/h2-console`, com:

- JDBC URL: `jdbc:h2:mem:testdb`
- usuário: `sa`
- senha: vazia

## Execução com Docker Compose

PostgreSQL e aplicação:

```shell
docker compose --profile postgresql up --build --wait
```

Esse perfil inicia `postgres` e `app-postgresql`; a aplicação ativa o perfil Spring `dev` e usa o hostname interno `postgres`.

MySQL e aplicação:

```shell
docker compose --profile mysql up --build --wait
```

Esse perfil inicia `mysql` e `app-mysql`; a aplicação ativa o perfil Spring `mysql` e usa o hostname interno `mysql`.

Para encerrar preservando os dados:

```shell
docker compose --profile postgresql down
docker compose --profile mysql down
```

Use `down --volumes` somente quando também quiser apagar os dados dos volumes locais.

Também é possível construir e executar somente a imagem da aplicação com H2:

```shell
docker build -t gestao-de-acao:local .
docker run --rm -p 8080:8080 -e TWELVE_DATA_API_KEY=test-key gestao-de-acao:local
```

## Docker com PostgreSQL do Windows

Este perfil inicia dois contêineres: `frontend` e `app-host-postgresql`. O banco não roda em contêiner; o backend acessa o PostgreSQL do Windows por `host.docker.internal`.

Mantenha os dois repositórios em diretórios irmãos com estes nomes, pois o contexto de build do frontend é `../gestao-acao-front`:

```text
SoftwareGestaoDeAcao/
|-- GestaoDeAcao/       # backend; contém este compose.yaml
`-- gestao-acao-front/  # frontend; contém Dockerfile e nginx.conf
```

Revisões mínimas compatíveis para este conjunto:

- Backend `GestaoDeAcao`: `05f2735b5c24790ef61e4e1830dd89eed1d43b87`, que inclui o Dockerfile, os perfis Spring e a API da carteira; use esta revisão do README/Compose ou um descendente compatível para obter a publicação apenas em localhost.
- Frontend `gestao-acao-front`: `26bc4517c4a9b0a2c25258e6537a69b6700b1e96`, que inclui a imagem Nginx, o proxy `/api` e a integração da carteira, ou um descendente compatível.

Execute os comandos abaixo a partir do diretório `GestaoDeAcao`. As portas do perfil `host-postgresql` são publicadas somente em `127.0.0.1`; `APP_PORT` e `FRONTEND_PORT` permitem alterar os números das portas.

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

## Endpoints

### Corretoras

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/corretoras` | Cadastra e valida uma corretora |
| `GET` | `/corretoras` | Lista corretoras |
| `GET` | `/corretoras/{id}` | Busca por id |
| `GET` | `/corretoras/cnpj/{cnpj}` | Busca por CNPJ normalizado |

Exemplo de cadastro:

```json
{
  "cnpj": "00.000.000/0001-91",
  "cep": "01001-000"
}
```

### Ações

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/acoes` | Cadastra uma ação vinculada a uma corretora |
| `GET` | `/acoes` | Lista ações |
| `GET` | `/acoes/{id}` | Busca por id |
| `GET` | `/acoes/ticker/{ticker}` | Busca por ticker normalizado |
| `PUT` | `/acoes/{id}/atualizar-cotacao` | Atualiza a cotação pelo provedor do mercado |

Exemplo de cadastro:

```json
{
  "ticker": "PETR4",
  "corretoraId": 1
}
```

### Operações

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/operacoes` | Lista todas as operações |
| `POST` | `/operacoes/comprar/{acaoId}?quantidade=10&precoCompra=42.50` | Registra uma compra; `precoCompra` é opcional e usa a cotação atual quando omitido |
| `POST` | `/operacoes/vender/{acaoId}?quantidade=5&precoVenda=42.50` | Registra uma venda |
| `GET` | `/operacoes/{acaoId}` | Lista o histórico de uma ação |
| `GET` | `/operacoes/compras` | Lista compras |
| `GET` | `/operacoes/vendas` | Lista vendas |
| `GET` | `/operacoes/buscar/{id}` | Busca uma operação por id |

## Erros HTTP

Erros conhecidos usam `Content-Type: application/problem+json` e o formato `ProblemDetail`. Erros de campos incluem a propriedade adicional `errors`.

| Status | Situação |
| --- | --- |
| `400 Bad Request` | Entrada malformada, validação ou operação inválida |
| `404 Not Found` | Corretora, ação ou operação inexistente |
| `409 Conflict` | CNPJ/ticker duplicado ou venda acima da posição disponível |
| `422 Unprocessable Entity` | CNPJ, CEP ou ticker válido no formato, mas ausente na fonte externa |
| `502 Bad Gateway` | Provedor retornou erro ou conteúdo inválido |
| `503 Service Unavailable` | Timeout ou indisponibilidade de provedor obrigatório |

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Requisição inválida",
  "status": 400,
  "detail": "Um ou mais campos da requisição são inválidos",
  "instance": "/acoes",
  "errors": {
    "ticker": ["Ticker é obrigatório"]
  }
}
```

## Fontes externas

- BrasilAPI: dados cadastrais do CNPJ
- ViaCEP: endereço pelo CEP
- Brapi: dados e cotações de ações brasileiras
- Twelve Data: dados e cotações de ações dos Estados Unidos
- CVM: cadastro diário oficial de Participantes Intermediários (`cad_intermed.zip`)

As chamadas possuem timeouts e tradução de falhas para respostas seguras. O cadastro da CVM é mantido em cache pelo período configurado em `CVM_CACHE_TTL`.

## Migração do banco

O Liquibase cria o schema e aplica a renomeação da tabela legada `produto` para `acoes`, além das restrições únicas de ticker e CNPJ. Para bancos existentes, siga o procedimento de backup, validação e rollback em [docs/database-migration.md](docs/database-migration.md).

## Testes e empacotamento

No PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Os testes automatizados usam H2, fixtures e servidores HTTP simulados; não exigem Docker, internet ou credenciais reais.
