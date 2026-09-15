# Endurecimento do Backend — Desenho Técnico

## Objetivo

Tornar a API de gestão de ações segura para versionamento, previsível para consumidores HTTP e verificável por testes automatizados. A mudança cobre validação de entrada, erros padronizados, integrações externas resilientes, validação real de intermediários na CVM e configuração dos bancos H2, PostgreSQL e MySQL.

## Escopo

### Incluído

- Remover a chave da Twelve Data do repositório e carregá-la por `TWELVE_DATA_API_KEY`.
- Validar DTOs e parâmetros HTTP com Jakarta Bean Validation.
- Normalizar CNPJ, CEP e ticker antes de consultar duplicidade ou serviços externos.
- Substituir `RuntimeException` genérica por exceções de domínio com códigos HTTP coerentes.
- Retornar erros no formato `ProblemDetail`, incluindo detalhes dos campos inválidos.
- Reutilizar o `RestClient.Builder` configurado pelo Spring Boot, com timeouts globais.
- Traduzir falhas HTTP, respostas incompletas e indisponibilidade das APIs externas.
- Validar o CNPJ da corretora contra o cadastro diário oficial de Participantes Intermediários da CVM.
- Corrigir a tabela da entidade `Acao` de `produto` para `acoes` e criar restrições únicas para CNPJ e ticker.
- Manter H2 como perfil padrão de desenvolvimento e oferecer perfis PostgreSQL e MySQL configurados por ambiente.
- Adicionar Dockerfile, Compose para os bancos, `.env.example` e documentação de execução.
- Cobrir regras de negócio, validações e mapeamento de erros com testes sem dependência da rede.

### Não incluído

- Scraping do StatusInvest ou Investidor10. Esses sites não fornecem uma API pública estável adequada para contrato de backend.
- Revogação automática da chave exposta. O proprietário da chave deve revogá-la no provedor.
- Frontend, autenticação de usuários ou mudanças no modelo de carteira além das validações necessárias.

## Arquitetura

### Contrato HTTP e validação

`AcaoRequestDTO` exigirá ticker no formato aceito e `corretoraId` positivo. `CorretoraRequestDTO` exigirá CNPJ com 14 dígitos e CEP com 8 dígitos, aceitando pontuação apenas para normalização. Os controllers usarão `@Valid`; parâmetros de compra e venda usarão `@Positive` e os resources usarão `@Validated`.

Uma `ApiExceptionHandler` anotada com `@RestControllerAdvice` produzirá `ProblemDetail`. Os status serão:

- `400 Bad Request`: entrada inválida, resposta externa inconsistente ou regra inválida de operação;
- `404 Not Found`: ação, corretora ou operação inexistente;
- `409 Conflict`: CNPJ ou ticker duplicado e venda acima da posição disponível;
- `422 Unprocessable Entity`: CNPJ, CEP ou ticker inexistente nos provedores consultados;
- `502 Bad Gateway`: provedor externo respondeu com erro ou conteúdo inválido;
- `503 Service Unavailable`: timeout ou indisponibilidade de provedor obrigatório.

### Clientes externos

Os quatro clientes atuais e o novo cliente CVM receberão `RestClient.Builder` por injeção e criarão clientes com URL base própria. Os timeouts virão das propriedades `spring.http.clients.connect-timeout` e `spring.http.clients.read-timeout`.

Cada cliente terá tipos de resposta mínimos para os campos realmente usados. O serviço não manipulará `Map` cru. Erros de status e transporte serão convertidos para uma exceção de integração que preserve o nome do provedor sem expor chaves, URLs completas ou resposta sensível.

### Validação CVM

A fonte será o conjunto oficial `cad_intermed.zip`, atualizado diariamente pela CVM. Um componente carregará o ZIP, localizará os CSVs e indexará CNPJs normalizados de intermediários ativos. O índice ficará em memória com validade configurável de 24 horas para evitar baixar o arquivo em cada cadastro.

No cadastro de corretora:

1. validar e normalizar CNPJ e CEP;
2. consultar CEP e dados cadastrais do CNPJ;
3. consultar o índice oficial da CVM;
4. rejeitar CNPJ ausente ou inativo;
5. persistir `validadaNaCvm=true` somente após correspondência verificável.

Se a CVM estiver indisponível e não houver cache válido, o cadastro falhará com `503`; a aplicação não presumirá validação.

### Persistência e configuração

O perfil padrão `h2` continuará adequado a desenvolvimento e testes. `postgresql` e `mysql` usarão `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, sem credenciais reais no repositório. O MySQL terá driver runtime explícito. O schema continuará gerenciado pelo Hibernate para manter o escopo acadêmico, com `ddl-auto` configurável e padrão `update` apenas nos perfis de desenvolvimento.

As colunas `acao.ticker` e `corretora.cnpj` terão unicidade no banco, complementando a verificação do serviço contra condições de corrida. A tabela da ação será `acoes`; a alteração local `@Table(name = "produto")` será substituída por esse nome por não representar a entidade.

### Regras de operações

Compra exigirá quantidade positiva e cotação atual positiva. Venda exigirá quantidade e preço positivos, uma posição comprada existente e saldo suficiente. O cálculo existente de preço médio e lucro/prejuízo será preservado, com testes que fixem o comportamento esperado.

### Testes

Os testes serão divididos em:

- testes unitários de `AcaoService`, `CorretoraService` e `OperacaoAcaoService`, com repositórios e gateways externos simulados;
- testes MVC para validação e `ProblemDetail`;
- testes de parsing do cadastro CVM usando um ZIP pequeno criado no próprio teste;
- teste de contexto usando H2 e chave fictícia, sem chamadas externas;
- execução completa com `./mvnw test` e empacotamento com `./mvnw package`.

Nenhum teste de rotina dependerá de internet, Docker ou credenciais reais.

## Operação e documentação

`.env.example` listará somente valores fictícios. `.env` será ignorado pelo Git. O README documentará perfis, variáveis, endpoints, formato de erro e fontes externas. O Compose disponibilizará PostgreSQL e MySQL como perfis opcionais; a aplicação continuará executável localmente com H2 sem Docker.

## Critérios de aceite

- Nenhum segredo real permanece em arquivos versionados.
- Entradas inválidas são rejeitadas antes de acessar banco ou rede.
- Erros HTTP possuem status e corpo `ProblemDetail` estáveis.
- Uma corretora só recebe `validadaNaCvm=true` após constar como ativa no cadastro oficial.
- Falhas externas não viram erro 500 genérico nem imprimem stack trace manualmente.
- H2, PostgreSQL e MySQL possuem configuração documentada e externalizada.
- Regras centrais têm testes automatizados e a suíte completa passa sem rede.
- O projeto é empacotado com sucesso pelo Maven Wrapper.
