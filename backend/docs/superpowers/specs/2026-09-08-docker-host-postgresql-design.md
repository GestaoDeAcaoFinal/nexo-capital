# Docker com PostgreSQL local — desenho de implantação

## Objetivo

Executar o frontend Angular e o backend Spring Boot em contêineres Docker separados, mantendo o PostgreSQL 16 instalado no Windows como fonte única dos dados já existentes no database `gestaodeacao`.

O processo não criará, importará, substituirá nem apagará o banco local. O pgAdmin continuará administrando o mesmo servidor PostgreSQL.

## Arquitetura

```text
Navegador
    |
    | http://localhost:4200
    v
Frontend Angular + Nginx (container)
    |
    | /api/* -> rede interna do Compose
    v
Backend Spring Boot (container:8080)
    |
    | jdbc:postgresql://host.docker.internal:5432/gestaodeacao
    v
PostgreSQL 16 no Windows (dados existentes)
```

O frontend será o ponto de entrada do navegador. O Nginx entregará os arquivos estáticos do Angular e encaminhará requisições iniciadas por `/api/` para o backend, removendo o prefixo antes de enviar a requisição. Assim, `/api/operacoes/carteira` chegará ao Spring como `/operacoes/carteira`.

Como frontend e API serão acessados pela mesma origem, esse fluxo não dependerá de novas permissões CORS. O backend continuará exposto em `localhost:8080` para testes diretos.

## Serviços do Compose

### `frontend`

- Imagem construída em duas etapas: Node para compilar o Angular e Nginx para servir o resultado.
- Porta padrão do host: `4200`, encaminhada para a porta `80` do contêiner.
- URL de produção da API: `/api`.
- Proxy interno para `app-host-postgresql:8080`.
- Roteamento SPA com fallback para `index.html`.

### `app-host-postgresql`

- Usa o Dockerfile já existente do backend.
- Ativa o perfil Spring `dev`, que usa o driver PostgreSQL.
- Conecta ao host pelo nome especial do Docker Desktop `host.docker.internal`.
- Usa o database `gestaodeacao` por padrão.
- Recebe usuário, senha, porta e nome do banco por variáveis do `.env`.
- Usa `DDL_AUTO=validate` para impedir que o Hibernate recrie as tabelas.

### PostgreSQL

O perfil novo não iniciará um contêiner PostgreSQL. O serviço PostgreSQL 16 do Windows, atualmente ouvindo na porta `5432`, continuará armazenando os dados.

O perfil `postgresql` já existente, com banco e volume Docker próprios, será preservado como alternativa isolada. Ele não deverá ser iniciado ao mesmo tempo que o perfil conectado ao host, pois ambos podem disputar as portas da aplicação e do banco.

## Configuração e segredos

O `.env` conterá apenas valores locais e continuará ignorado pelo Git. A documentação e o `.env.example` mostrarão nomes e valores fictícios, nunca a senha real.

Variáveis previstas para o perfil do PostgreSQL do Windows:

- `APP_PORT`, padrão `8080`;
- `FRONTEND_PORT`, padrão `4200`;
- `HOST_POSTGRES_PORT`, padrão `5432`;
- `POSTGRES_DB`, padrão `gestaodeacao`;
- `DB_USERNAME`;
- `DB_PASSWORD`;
- `TWELVE_DATA_API_KEY`, opcional para iniciar e necessária para ativos dos EUA.

O PostgreSQL deve aceitar conexões TCP vindas da rede do Docker Desktop. Ele já está ouvindo em todas as interfaces na porta `5432`; durante a validação será verificado se o `pg_hba.conf` também autoriza a conexão. Se não autorizar, será proposta a regra mínima para a sub-rede efetivamente usada pelo Docker, sem liberar acesso público indiscriminado.

## Persistência e segurança dos dados

- Nenhum volume Docker será montado sobre o diretório de dados do PostgreSQL do Windows.
- Nenhum comando de exclusão ou recriação do database será executado.
- O Liquibase aplicará somente changesets pendentes do projeto; o Hibernate validará o schema.
- Antes da primeira inicialização conectada ao banco existente, será confirmada a existência de um backup recente ou solicitada autorização para criar um com `pg_dump`.
- A validação do banco será somente leitura: consultará registros existentes sem inserir, atualizar ou excluir dados.

## Arquivos previstos

No frontend:

- `Dockerfile` para build Angular e runtime Nginx;
- `.dockerignore`;
- `nginx.conf` para SPA e proxy `/api`;
- `src/environments/environment.production.ts` para usar `/api`.

No backend:

- `compose.yaml` com os serviços `frontend` e `app-host-postgresql` em um perfil próprio;
- `.env.example` com as novas variáveis sem credenciais reais;
- `README.md` com comandos de inicialização, encerramento, backup e diagnóstico.

## Inicialização e encerramento

O conjunto será iniciado pelo perfil `host-postgresql`, com um único comando do Compose, build e execução em segundo plano. O comando exato será documentado no README.

O encerramento usará `docker compose down` sem `--volumes`. Como o banco está fora do Docker, parar ou reconstruir os contêineres não removerá os dados do PostgreSQL local.

## Tratamento de falhas

- Credenciais inválidas: o backend falhará com erro de autenticação; a senha não será exibida no relatório.
- PostgreSQL inacessível: serão verificados serviço, porta, firewall, `listen_addresses` e `pg_hba.conf`.
- Porta `8080` ou `4200` ocupada: será identificado o processo antes de qualquer encerramento; portas alternativas poderão ser informadas no `.env`.
- Schema incompatível: a inicialização será interrompida, preservando os dados, e o erro do Liquibase/Hibernate será analisado antes de qualquer mudança no banco.
- Backend ainda indisponível: o Nginx poderá responder `502` temporariamente até o backend concluir a inicialização; reiniciar o frontend não será necessário.

## Validação

1. Executar todos os testes Maven do backend.
2. Executar os testes e o build de produção do Angular.
3. Construir as duas imagens Docker.
4. Iniciar somente o perfil que usa o PostgreSQL do Windows.
5. Confirmar que existem exatamente os contêineres esperados de frontend e backend para esse perfil.
6. Consultar a API diretamente e pelo proxy `/api`.
7. Comparar uma contagem ou lista conhecida do database `gestaodeacao` com a resposta da API, sem expor conteúdo sensível.
8. Reiniciar os contêineres e confirmar que os mesmos dados continuam disponíveis.
9. Inspecionar os logs para garantir ausência de falhas de autenticação, rede, Liquibase ou schema.

## Critérios de aceite

- Frontend acessível em `http://localhost:4200`.
- Backend acessível diretamente em `http://localhost:8080` e pelo frontend em `/api`.
- Frontend e backend executando em contêineres separados.
- Backend conectado ao database local `gestaodeacao` do PostgreSQL do Windows.
- Dados existentes visíveis na aplicação antes e depois de reiniciar os contêineres.
- Nenhuma credencial versionada e nenhum dado existente apagado ou substituído.
