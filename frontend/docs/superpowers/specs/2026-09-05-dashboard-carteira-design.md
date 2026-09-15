# Design do dashboard de carteira de ações

## Objetivo

Transformar a interface atual em uma aplicação financeira coerente, confiável e responsiva, inspirada na hierarquia visual do Investidor10, sem copiar sua marca ou introduzir dados que o backend não fornece. O redesign preserva todas as rotas, contratos HTTP, validações e comportamentos implementados.

## Referência visual

O Investidor10 será usado como referência para densidade informacional, navegação financeira, destaque de ativos e organização em cards e tabelas. A implementação terá identidade própria, com o nome “Carteira Ações”, paleta e componentes locais.

Referência: https://investidor10.com.br/

## Direção visual

- Cabeçalho azul-marinho profundo com marca, navegação e ação principal.
- Fundo geral cinza-claro azulado para separar áreas de conteúdo.
- Verde esmeralda como cor de ação, confirmação e indicadores positivos.
- Vermelho controlado para erros e resultados negativos.
- Cards brancos com bordas suaves, cantos arredondados e sombras discretas.
- Tipografia de sistema moderna, com números financeiros usando algarismos tabulares.
- Conteúdo central limitado em largura para manter leitura confortável em telas grandes.

## Arquitetura da interface

### Shell global

O `AppComponent` passa a hospedar um cabeçalho persistente e responsivo acima do `router-outlet`. A navegação terá links para Visão geral, Corretoras, Ações e Operações, além de um botão de destaque para registrar uma nova operação. O shell não introduzirá autenticação, perfil ou notificações fictícias.

### Página inicial

A home funcionará como porta de entrada da carteira. Terá uma apresentação curta, três cards principais para Corretoras, Ações e Operações e atalhos secundários para os seis fluxos existentes. Como não existe endpoint agregado, a tela não exibirá patrimônio, rentabilidade ou saldos inventados.

### Formulários

Os três formulários usarão o mesmo padrão visual: cabeçalho da página, breadcrumb simples, painel principal, agrupamento lógico dos campos, texto auxiliar, validação próxima ao campo e área contextual para sucesso ou erro. Botões manterão indicação textual de processamento e bloqueio de duplicidade.

### Listagens e consultas

As páginas de listagem terão cabeçalho com ação primária, painel de busca ou filtros e tabela responsiva. Tickers, tipos de operação e estados serão representados por badges textuais. Valores monetários terão alinhamento e fonte tabular. Loading, vazio, erro e retry permanecerão explícitos.

## Sistema de estilos

O arquivo `src/styles.css` concentrará tokens CSS e componentes visuais compartilhados: cores, raios, sombras, tipografia, shell, botões, cards, formulários, alertas, badges e tabelas. Os CSS de página serão usados somente quando houver necessidade específica, evitando duplicação.

As classes utilitárias existentes do Bootstrap continuarão disponíveis, mas a identidade visual será aplicada por classes semânticas próprias. Nenhuma nova biblioteca visual ou pacote de ícones será adicionado.

## Responsividade e acessibilidade

- Layout fluido entre celular, tablet e desktop.
- Navegação compactada em telas estreitas sem esconder destinos essenciais.
- Tabelas com rolagem horizontal e células prioritárias legíveis.
- Contraste suficiente para texto, bordas e estados.
- Foco de teclado visível em links, botões e controles.
- Estados nunca comunicados apenas por cor.
- Respeito a `prefers-reduced-motion` nas transições decorativas.

## Arquivos afetados

- `src/styles.css`
- `src/app/app.component.html`
- `src/app/app.component.css`
- `src/app/app.component.ts`, apenas se necessário para navegação responsiva
- Home e templates das seis páginas em `src/app/pages/`
- CSS específicos das páginas quando o estilo global não for suficiente
- Specs de componentes somente quando a nova estrutura exigir atualização de seletores ou novas verificações de acessibilidade

## Preservação funcional

O redesign não altera serviços, modelos, endpoints, regras de validação, estados assíncronos ou rotas. Todos os formulários reativos, buscas, filtros, atualização de cotação, compra e venda continuam usando os métodos atuais.

## Verificação

- Executar `npm test -- --watch=false --browsers=ChromeHeadless`.
- Executar `npm run build`.
- Verificar ausência de texto corrompido e de regressões em loading, vazio, erro e sucesso.
- Inspecionar responsividade das rotas principais quando um navegador integrado estiver disponível.

## Fora de escopo

- Gráficos, patrimônio consolidado, rentabilidade ou dividendos sem suporte do backend.
- Autenticação, conta de usuário ou funcionalidades PRO.
- Cópia de logotipo, textos, imagens ou componentes proprietários do Investidor10.
- Alterações no backend.
