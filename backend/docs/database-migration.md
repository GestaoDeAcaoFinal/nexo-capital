# Migração de `produto` para `acoes`

O changeset `003-renomear-produto-e-adicionar-unicidade.xml`:

- renomeia `produto` para `acoes` sem copiar ou descartar linhas;
- preserva `operacao_acao.acao_id` e recria sua chave estrangeira para `acoes`;
- recria a chave de `acoes.corretora_id` para `corretora`;
- adiciona unicidade a `acoes.ticker` e `corretora.cnpj`.

O procedimento abaixo deve ser executado com as escritas da aplicação
interrompidas. Em ambiente persistente, não prossiga sem um backup válido.

## Preparação do Liquibase

Os comandos usam a mesma versão do Liquibase resolvida pelo projeto (`5.0.2`)
em um container, portanto não exigem o CLI instalado localmente. A imagem
oficial não inclui os drivers JDBC; copie-os das dependências Maven para uma
pasta ignorada pelo Git:

```powershell
mvn --% dependency:copy-dependencies -DincludeArtifactIds=postgresql,mysql-connector-j -DoutputDirectory=target/liquibase-drivers

$resources = (Resolve-Path "src/main/resources").Path
$drivers = (Resolve-Path "target/liquibase-drivers").Path
$composeProject = "gestao-de-acao"
$network = "${composeProject}_default"
```

Defina no terminal as mesmas credenciais usadas pelo Compose ou pelo banco de
destino. Não grave credenciais reais no repositório:

```powershell
$env:POSTGRES_DB = "gestaodeacao"
$env:POSTGRES_USER = "gestao"
$env:POSTGRES_PASSWORD = "<senha-postgresql>"

$env:MYSQL_DATABASE = "gestaodeacao"
$env:MYSQL_USER = "gestao"
$env:MYSQL_PASSWORD = "<senha-mysql>"
$env:MYSQL_ROOT_PASSWORD = "<senha-root-mysql>"
```

O perfil Spring responsável pelo PostgreSQL neste projeto é `dev`, configurado
por `application-dev.properties`. O perfil do MySQL é `mysql`.

## Verificações anteriores ao backup

Inicie somente o banco desejado:

```powershell
docker compose --project-name $composeProject --profile postgresql up --detach --wait postgres
# ou
docker compose --project-name $composeProject --profile mysql up --detach --wait mysql
```

Confira duplicidades no schema legado. As duas consultas devem retornar zero
linhas; se retornarem dados, corrija-os antes de aplicar a constraint única.

PostgreSQL:

```powershell
docker compose --project-name $composeProject exec -T postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -v ON_ERROR_STOP=1 -c "SELECT ticker, COUNT(*) FROM produto GROUP BY ticker HAVING COUNT(*) > 1;" -c "SELECT cnpj, COUNT(*) FROM corretora GROUP BY cnpj HAVING COUNT(*) > 1;"
```

MySQL:

```powershell
docker compose --project-name $composeProject exec -T mysql mysql -u $env:MYSQL_USER "-p$env:MYSQL_PASSWORD" $env:MYSQL_DATABASE -e "SELECT ticker, COUNT(*) FROM produto GROUP BY ticker HAVING COUNT(*) > 1; SELECT cnpj, COUNT(*) FROM corretora GROUP BY cnpj HAVING COUNT(*) > 1;"
```

Registre também as contagens que deverão ser preservadas:

```sql
SELECT COUNT(*) AS corretoras FROM corretora;
SELECT COUNT(*) AS produtos FROM produto;
SELECT COUNT(*) AS operacoes_ligadas
FROM operacao_acao o JOIN produto p ON p.id = o.acao_id;
```

## PostgreSQL

### Backup

O formato customizado (`-Fc`) permite restaurar o dump com `pg_restore`:

```powershell
New-Item -ItemType Directory -Force -Path "target/migration-backups" | Out-Null
docker compose --project-name $composeProject exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/gestaodeacao-pre-003.dump'
docker compose --project-name $composeProject cp postgres:/tmp/gestaodeacao-pre-003.dump ./target/migration-backups/gestaodeacao-pre-003.dump
Get-Item ./target/migration-backups/gestaodeacao-pre-003.dump
```

Guarde o arquivo fora do host do banco e confirme que seu tamanho é maior que
zero antes do rollout.

### Aplicação do changeset `003`

```powershell
docker run --rm --network $network `
  --mount "type=bind,source=$resources,target=/liquibase/changelog,readonly" `
  --mount "type=bind,source=$drivers,target=/liquibase/lib,readonly" `
  liquibase/liquibase:5.0.2 `
  --url="jdbc:postgresql://postgres:5432/$env:POSTGRES_DB" `
  --username="$env:POSTGRES_USER" `
  --password="$env:POSTGRES_PASSWORD" `
  --changelog-file=db/changelog/db.changelog-master.xml update
```

Depois da migração, a aplicação pode ser iniciada com `SPRING_PROFILES_ACTIVE=dev`
e `DDL_AUTO=validate`.

### Validação

Compare as contagens com as registradas antes do backup e confira as quatro
constraints e seus destinos:

```powershell
docker compose --project-name $composeProject exec -T postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -v ON_ERROR_STOP=1 -c "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM acoes) AS acoes, (SELECT COUNT(*) FROM operacao_acao o JOIN acoes a ON a.id = o.acao_id) AS operacoes_ligadas, to_regclass('public.produto') IS NULL AS produto_removida;" -c "SELECT conname, contype, conrelid::regclass AS tabela, confrelid::regclass AS referencia FROM pg_constraint WHERE conname IN ('uk_acoes_ticker','uk_corretora_cnpj','fk_acoes_corretora','fk_operacao_acao_acao') ORDER BY conname;"
```

O resultado esperado contém `produto_removida = true`, as mesmas contagens e:

| Constraint | Tipo | Tabela | Referência |
|---|---|---|---|
| `fk_acoes_corretora` | FK | `acoes` | `corretora` |
| `fk_operacao_acao_acao` | FK | `operacao_acao` | `acoes` |
| `uk_acoes_ticker` | UNIQUE | `acoes` | — |
| `uk_corretora_cnpj` | UNIQUE | `corretora` | — |

## MySQL

### Backup

```powershell
New-Item -ItemType Directory -Force -Path "target/migration-backups" | Out-Null
docker compose --project-name $composeProject exec -T mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction "$MYSQL_DATABASE" > /tmp/gestaodeacao-pre-003.sql'
docker compose --project-name $composeProject cp mysql:/tmp/gestaodeacao-pre-003.sql ./target/migration-backups/gestaodeacao-pre-003.sql
Get-Item ./target/migration-backups/gestaodeacao-pre-003.sql
```

### Aplicação do changeset `003`

```powershell
docker run --rm --network $network `
  --mount "type=bind,source=$resources,target=/liquibase/changelog,readonly" `
  --mount "type=bind,source=$drivers,target=/liquibase/lib,readonly" `
  liquibase/liquibase:5.0.2 `
  --url="jdbc:mysql://mysql:3306/$env:MYSQL_DATABASE" `
  --username="$env:MYSQL_USER" `
  --password="$env:MYSQL_PASSWORD" `
  --changelog-file=db/changelog/db.changelog-master.xml update
```

Depois da migração, a aplicação pode ser iniciada com
`SPRING_PROFILES_ACTIVE=mysql` e `DDL_AUTO=validate`.

### Validação

```powershell
docker compose --project-name $composeProject exec -T mysql mysql -u $env:MYSQL_USER "-p$env:MYSQL_PASSWORD" $env:MYSQL_DATABASE -e "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM acoes) AS acoes, (SELECT COUNT(*) FROM operacao_acao o JOIN acoes a ON a.id = o.acao_id) AS operacoes_ligadas, (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'produto') AS produto_restante; SELECT tc.CONSTRAINT_NAME, tc.CONSTRAINT_TYPE, tc.TABLE_NAME, kcu.REFERENCED_TABLE_NAME FROM information_schema.TABLE_CONSTRAINTS tc LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND kcu.TABLE_NAME = tc.TABLE_NAME AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME WHERE tc.CONSTRAINT_SCHEMA = DATABASE() AND tc.CONSTRAINT_NAME IN ('uk_acoes_ticker','uk_corretora_cnpj','fk_acoes_corretora','fk_operacao_acao_acao') ORDER BY tc.CONSTRAINT_NAME;"
```

O resultado esperado contém `produto_restante = 0`, as mesmas contagens e as
quatro constraints descritas na tabela da seção PostgreSQL.

## Rollback controlado

Interrompa novamente as escritas e restaure primeiro a versão anterior da
aplicação, pois ela ainda mapeia `produto`. Execute somente um rollback, que
corresponde ao changeset `003`.

PostgreSQL:

```powershell
docker run --rm --network $network `
  --mount "type=bind,source=$resources,target=/liquibase/changelog,readonly" `
  --mount "type=bind,source=$drivers,target=/liquibase/lib,readonly" `
  liquibase/liquibase:5.0.2 `
  --url="jdbc:postgresql://postgres:5432/$env:POSTGRES_DB" `
  --username="$env:POSTGRES_USER" `
  --password="$env:POSTGRES_PASSWORD" `
  --changelog-file=db/changelog/db.changelog-master.xml `
  rollback-count --count=1
```

MySQL:

```powershell
docker run --rm --network $network `
  --mount "type=bind,source=$resources,target=/liquibase/changelog,readonly" `
  --mount "type=bind,source=$drivers,target=/liquibase/lib,readonly" `
  liquibase/liquibase:5.0.2 `
  --url="jdbc:mysql://mysql:3306/$env:MYSQL_DATABASE" `
  --username="$env:MYSQL_USER" `
  --password="$env:MYSQL_PASSWORD" `
  --changelog-file=db/changelog/db.changelog-master.xml `
  rollback-count --count=1
```

Após o rollback, repita as contagens usando `produto`. Confirme que `acoes` não
existe, que as constraints únicas novas não existem e que estas FKs voltaram:

| Constraint | Tabela | Referência |
|---|---|---|
| `fk_produto_corretora` | `produto` | `corretora` |
| `fk_operacao_acao_produto` | `operacao_acao` | `produto` |

PostgreSQL:

```powershell
docker compose --project-name $composeProject exec -T postgres psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -v ON_ERROR_STOP=1 -c "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM produto) AS produtos, (SELECT COUNT(*) FROM operacao_acao o JOIN produto p ON p.id = o.acao_id) AS operacoes_ligadas, to_regclass('public.acoes') IS NULL AS acoes_removida;" -c "SELECT conname, conrelid::regclass AS tabela, confrelid::regclass AS referencia FROM pg_constraint WHERE conname IN ('fk_produto_corretora','fk_operacao_acao_produto','uk_acoes_ticker','uk_corretora_cnpj') ORDER BY conname;"
```

MySQL:

```powershell
docker compose --project-name $composeProject exec -T mysql mysql -u $env:MYSQL_USER "-p$env:MYSQL_PASSWORD" $env:MYSQL_DATABASE -e "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM produto) AS produtos, (SELECT COUNT(*) FROM operacao_acao o JOIN produto p ON p.id = o.acao_id) AS operacoes_ligadas, (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acoes') AS acoes_restante; SELECT tc.CONSTRAINT_NAME, tc.TABLE_NAME, kcu.REFERENCED_TABLE_NAME FROM information_schema.TABLE_CONSTRAINTS tc LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND kcu.TABLE_NAME = tc.TABLE_NAME AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME WHERE tc.CONSTRAINT_SCHEMA = DATABASE() AND tc.CONSTRAINT_NAME IN ('fk_produto_corretora','fk_operacao_acao_produto','uk_acoes_ticker','uk_corretora_cnpj') ORDER BY tc.CONSTRAINT_NAME;"
```

## Restauração do backup

Prefira restaurar em outro banco e validar o conteúdo antes de alterar a
configuração da aplicação. Os nomes abaixo são exemplos; use nomes novos para
evitar sobrescrever um banco existente.

PostgreSQL:

```powershell
docker compose --project-name $composeProject cp ./target/migration-backups/gestaodeacao-pre-003.dump postgres:/tmp/gestaodeacao-pre-003.dump
docker compose --project-name $composeProject exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" gestaodeacao_restore'
docker compose --project-name $composeProject exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d gestaodeacao_restore /tmp/gestaodeacao-pre-003.dump'
docker compose --project-name $composeProject exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d gestaodeacao_restore -v ON_ERROR_STOP=1 -c "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM produto) AS produtos, (SELECT COUNT(*) FROM operacao_acao o JOIN produto p ON p.id = o.acao_id) AS operacoes_ligadas, (SELECT COUNT(*) FROM databasechangelog) AS changesets;"'
```

MySQL:

```powershell
docker compose --project-name $composeProject cp ./target/migration-backups/gestaodeacao-pre-003.sql mysql:/tmp/gestaodeacao-pre-003.sql
docker compose --project-name $composeProject exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE gestaodeacao_restore;"'
docker compose --project-name $composeProject exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" gestaodeacao_restore < /tmp/gestaodeacao-pre-003.sql'
docker compose --project-name $composeProject exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" gestaodeacao_restore -e "SELECT (SELECT COUNT(*) FROM corretora) AS corretoras, (SELECT COUNT(*) FROM produto) AS produtos, (SELECT COUNT(*) FROM operacao_acao o JOIN produto p ON p.id = o.acao_id) AS operacoes_ligadas, (SELECT COUNT(*) FROM DATABASECHANGELOG) AS changesets;"'
```

## Verificação executada em bancos descartáveis

Em 3 de setembro de 2026, o procedimento foi exercitado com
`postgres:17-alpine`, `mysql:8.4` e `liquibase/liquibase:5.0.2`, usando projetos
Compose isolados e volumes novos. Em ambos os bancos foi confirmado:

1. baseline com os changesets `001` e `002`;
2. uma corretora, um produto e uma operação ligada antes da migração;
3. backup anterior ao `003` criado com conteúdo;
4. renomeação para `acoes`, preservação das três contagens e quatro constraints;
5. rollback para `produto`, preservação das contagens e restauração das FKs;
6. restauração do backup em outro banco com os dois changesets originais.

Para repetir o ensaio sem afetar os volumes usuais, use outro nome de projeto e
uma porta livre. Ao terminar, remova apenas esse projeto explicitamente:

```powershell
$env:POSTGRES_PORT = "15433"
docker compose --project-name gda-migration-pg --profile postgresql up --detach --wait postgres
docker compose --project-name gda-migration-pg --profile postgresql down --volumes --remove-orphans

$env:MYSQL_PORT = "13307"
docker compose --project-name gda-migration-mysql --profile mysql up --detach --wait mysql
docker compose --project-name gda-migration-mysql --profile mysql down --volumes --remove-orphans
```

`down --volumes` é destrutivo e deve ser usado somente nos projetos descartáveis
nomeados acima, nunca no projeto que contém os dados reais.
