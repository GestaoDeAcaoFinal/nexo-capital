# Nexo Capital — design institucional do frontend

## Objetivo

Transformar o frontend existente em uma aplicação financeira mais séria, consistente e profissional, preservando os fluxos e contratos de API atuais. A identidade visual passa a usar a marca **Nexo Capital**, a navegação permanece horizontal e a carteira passa a ter uma área própria, separada do extrato de operações.

## Escopo

O redesign cobre:

- Visão geral;
- Carteira;
- Corretoras e cadastro de corretora;
- Ações e cadastro de ação;
- Operações e registro de operação;
- cabeçalho, navegação, rodapé e estilos compartilhados;
- estados de carregamento, vazio, sucesso e erro;
- comportamento responsivo e acessibilidade;
- testes dos comportamentos e da estrutura alterados.

Não fazem parte desta mudança novos indicadores financeiros, novos endpoints, autenticação, edição ou exclusão de registros. As alterações locais já existentes no repositório devem ser preservadas e incorporadas sem regressão.

## Arquitetura de navegação

O cabeçalho exibirá a marca **Nexo Capital**, a ação primária “Nova operação” e cinco destinos:

1. **Visão geral** em `/`;
2. **Carteira** em `/carteira`;
3. **Corretoras** em `/corretoras`;
4. **Ações** em `/acoes`;
5. **Operações** em `/operacoes`.

A nova rota `/carteira` será atendida por um componente standalone próprio. Ela reutilizará `OperacaoService.resumirCarteira()` e assumirá toda a apresentação detalhada de posições que hoje está em `OperacaoListComponent`.

`OperacaoListComponent` deixará de buscar e renderizar o resumo da carteira. Permanecerão nele filtros, histórico por ação, consulta por ID e extrato de operações. Nenhum contrato HTTP será alterado.

As rotas de cadastro existentes serão mantidas:

- `/corretoras/nova`;
- `/acoes/nova`;
- `/operacoes/nova`.

## Sistema visual

A direção aprovada é **institucional horizontal**:

- azul-marinho profundo para cabeçalho, marca e títulos;
- fundo cinza muito claro e superfícies brancas;
- verde só para ações primárias, seleção ativa e resultados positivos;
- vermelho só para perdas, validações e erros;
- bordas discretas, sombras contidas e raios menores;
- tipografia sóbria, com hierarquia clara e números tabulares;
- densidade informacional de plataforma financeira, sem excesso de elementos decorativos.

Os estilos globais serão organizados em tokens e padrões reutilizáveis para páginas, cabeçalhos, painéis, métricas, tabelas, filtros, formulários, botões e estados assíncronos. Estilos específicos permanecerão junto do componente somente quando não forem compartilháveis.

## Visão geral

A página inicial funcionará como resumo executivo e conterá:

- valor atual da carteira;
- custo investido;
- resultado nominal;
- quantidade total em posse;
- composição por ativo;
- até cinco operações recentes;
- acessos objetivos à carteira e ao extrato completo.

Ela não repetirá a tabela completa da carteira nem os filtros de operações. Valores consolidados serão formatados em BRL; movimentações recentes continuarão na moeda original da ação.

## Carteira

A nova página concentrará:

- métricas consolidadas;
- quantidade total e número de posições;
- tabela completa por ativo;
- preço médio, custo total, cotação atual e valor atual;
- indicação textual de que ativos internacionais são convertidos para BRL pela cotação atual;
- atualização manual, estados de carregamento, vazio e erro.

O componente será responsável apenas pelo resumo da carteira. Não carregará histórico, filtros ou consulta de operações.

## Corretoras

A listagem manterá busca por ID ou CNPJ, resultado da consulta e tabela cadastrada. A apresentação será reorganizada em cabeçalho de página, painel de consulta compacto e tabela institucional. O cadastro usará o mesmo sistema de formulário, com validação e resultado de sucesso consistentes.

## Ações

A listagem manterá busca por ID ou ticker, atualização individual de cotação e tabela de ativos. Mercado e moeda terão leitura mais clara, e a ação de atualizar cotação terá prioridade secundária. O cadastro preservará a seleção de corretora e a consulta externa executada pelo backend.

## Operações

A página ficará dedicada a:

- filtros “Todas”, “Compras” e “Vendas”;
- histórico por ação;
- consulta direta por ID;
- tabela completa do extrato;
- acesso à nova operação.

Compra e venda continuarão no mesmo formulário. Ao escolher uma ação, o preço e os textos auxiliares devem refletir a moeda local do ativo. A página não exibirá mais o resumo da carteira.

## Estados, acessibilidade e responsividade

- Todo carregamento continuará exposto com `role="status"` e região ao vivo quando apropriado.
- Erros acionáveis usarão `role="alert"` e oferecerão nova tentativa quando a operação puder ser repetida.
- Estados vazios explicarão o próximo passo sem inventar dados.
- Navegação ativa continuará indicada semanticamente por `ariaCurrentWhenActive="page"`.
- Foco visível, rótulos, descrições e associação de erros aos campos serão preservados.
- Em telas estreitas, a navegação horizontal poderá rolar sem esconder destinos.
- Métricas e painéis serão empilhados progressivamente.
- Tabelas financeiras manterão rolagem horizontal controlada quando as colunas não couberem.

## Fluxo de dados

- `HomeComponent` continuará combinando `resumirCarteira()` e `listar()`.
- `CarteiraComponent` consumirá somente `resumirCarteira()`.
- `OperacaoListComponent` consumirá as consultas de operações e ações necessárias aos filtros.
- Corretoras e ações continuarão usando seus serviços atuais.
- Nenhum valor fictício será introduzido; a interface renderizará somente dados reais ou estados vazios.

## Estratégia de implementação

1. Consolidar identidade, navegação e tokens globais.
2. Criar a rota e o componente de carteira com a apresentação extraída de operações.
3. Simplificar operações após a extração.
4. Refinar a visão geral para o novo sistema visual.
5. Padronizar listas e formulários de corretoras, ações e operações.
6. Ajustar responsividade e acessibilidade.
7. Executar testes direcionados, suíte completa e build de produção.

## Testes e critérios de aceite

- O cabeçalho exibe “Nexo Capital” e os cinco destinos principais.
- `/carteira` renderiza os dados de `resumirCarteira()` e todos os estados assíncronos.
- `/operacoes` não solicita nem exibe o resumo da carteira.
- Visão geral mantém indicadores e operações recentes.
- Corretoras, ações e operações preservam busca, filtros, cadastro e atualização existentes.
- Valores de carteira permanecem em BRL e valores do histórico respeitam a moeda da ação.
- Navegação ativa, foco visível, rótulos e mensagens de estado permanecem acessíveis.
- Layouts de desktop e móvel não apresentam sobreposição ou conteúdo inacessível.
- Testes Angular passam sem falhas e `ng build` conclui com sucesso.
