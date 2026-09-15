# Dashboard de Carteira Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transformar o frontend existente em uma aplicação de carteira de ações com aparência profissional, responsiva e acessível, sem alterar integrações ou regras de negócio.

**Architecture:** O `AppComponent` fornecerá um shell persistente com navegação Angular e o `router-outlet`; os templates de página usarão um pequeno sistema de componentes visuais baseado em classes semânticas centralizadas em `src/styles.css`. Os componentes TypeScript, serviços, modelos, rotas e estados assíncronos permanecem funcionais, com mudanças de TypeScript limitadas aos imports de navegação exigidos pelo shell.

**Tech Stack:** Angular 19.2 standalone components, Angular Router, Reactive Forms, Bootstrap 5.3.8, CSS responsivo, Jasmine/Karma e Angular CLI.

**Spec:** `docs/superpowers/specs/2026-09-05-dashboard-carteira-design.md`

## Global Constraints

- Alterar somente o repositório `gestao-acao-front`; nenhum arquivo do backend entra no escopo.
- Preservar serviços, modelos, endpoints, regras de validação, estados assíncronos e rotas.
- Não adicionar bibliotecas visuais, pacote de ícones, autenticação, gráficos ou métricas sem suporte do backend.
- Não copiar logotipo, textos, imagens ou componentes proprietários do Investidor10.
- Manter loading, vazio, erro, retry e sucesso explícitos e nunca comunicados apenas por cor.
- Usar foco de teclado visível, contraste suficiente e `prefers-reduced-motion`.
- A navegação Angular 19 deve importar `RouterLink` e `RouterLinkActive` no componente standalone e usar `ariaCurrentWhenActive="page"`, conforme a documentação oficial consultada via Context7.

---

## File Structure

- `src/styles.css`: tokens de cor, tipografia, elevação, componentes compartilhados e breakpoints.
- `src/app/app.component.ts`: imports standalone do Angular Router usados pelo shell.
- `src/app/app.component.html`: cabeçalho global, marca, navegação, ação principal, conteúdo e rodapé.
- `src/app/app.component.css`: detalhes exclusivos do shell e navegação responsiva.
- `src/app/app.component.spec.ts`: contrato estrutural e acessível do shell.
- `src/app/pages/home/home.component.html`: apresentação da carteira, três cards de domínio e seis atalhos reais.
- `src/app/pages/home/home.component.css`: composição visual exclusiva da home.
- `src/app/pages/home/home.component.spec.ts`: hierarquia, cards e destinos da home.
- `src/app/pages/*-form/*.component.html`: cabeçalhos, breadcrumbs, painéis e feedback dos três formulários.
- `src/app/pages/*-form/*.component.spec.ts`: contratos estruturais sem alterar testes de regras de negócio.
- `src/app/pages/*-list/*.component.html`: cabeçalhos, filtros, estados, badges e tabelas das três listagens.
- `src/app/pages/*-list/*.component.spec.ts`: contratos estruturais e representação textual dos dados.
- `src/app/pages/*/*.component.css`: somente ajustes realmente exclusivos; preferir o sistema global.

### Task 1: Shell global e fundação visual

**Files:**
- Modify: `src/app/app.component.spec.ts`
- Modify: `src/app/app.component.ts`
- Modify: `src/app/app.component.html`
- Modify: `src/app/app.component.css`
- Modify: `src/styles.css`

**Interfaces:**
- Consumes: `routes` de `src/app/app.routes.ts` e o `router-outlet` existente.
- Produces: classes globais `.app-container`, `.page-shell`, `.surface-card`, `.page-heading`, `.eyebrow`, `.btn-finance-primary`, `.status-panel`, `.finance-table` e `.numeric`; shell com links `/`, `/corretoras`, `/acoes`, `/operacoes` e `/operacoes/nova`.

- [ ] **Step 1: Escrever os testes do shell que devem falhar**

Adicionar ao `app.component.spec.ts`:

```ts
it('expõe navegação financeira acessível e ação principal', () => {
  const fixture = TestBed.createComponent(AppComponent);
  fixture.detectChanges();
  const host = fixture.nativeElement as HTMLElement;
  const nav = host.querySelector('nav[aria-label="Navegação principal"]');
  const destinations = Array.from(nav?.querySelectorAll('a') ?? [])
    .map(link => link.getAttribute('href'));

  expect(nav).not.toBeNull();
  expect(destinations).toEqual(['/', '/corretoras', '/acoes', '/operacoes']);
  expect(host.querySelector('a[href="/operacoes/nova"]')?.textContent)
    .toContain('Nova operação');
  expect(host.querySelector('main.app-main router-outlet')).not.toBeNull();
});
```

- [ ] **Step 2: Executar o teste isolado e confirmar a falha**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/app.component.spec.ts`

Expected: FAIL porque o cabeçalho, a navegação e a ação principal ainda não existem.

- [ ] **Step 3: Importar as diretivas de navegação no componente standalone**

Atualizar `app.component.ts`:

```ts
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'Carteira Ações';
}
```

- [ ] **Step 4: Implementar o shell acessível**

Usar em `app.component.html` esta estrutura e manter exatamente os destinos testados:

```html
<header class="app-header">
  <div class="app-container app-header__inner">
    <a class="brand" routerLink="/" aria-label="Carteira Ações — visão geral">
      <span class="brand__mark" aria-hidden="true">CA</span>
      <span><strong>Carteira</strong><small>Ações</small></span>
    </a>
    <nav class="primary-nav" aria-label="Navegação principal">
      <a routerLink="/" routerLinkActive="is-active"
         [routerLinkActiveOptions]="{ exact: true }" ariaCurrentWhenActive="page">Visão geral</a>
      <a routerLink="/corretoras" routerLinkActive="is-active" ariaCurrentWhenActive="page">Corretoras</a>
      <a routerLink="/acoes" routerLinkActive="is-active" ariaCurrentWhenActive="page">Ações</a>
      <a routerLink="/operacoes" routerLinkActive="is-active" ariaCurrentWhenActive="page">Operações</a>
    </nav>
    <a class="btn-finance-primary header-action" routerLink="/operacoes/nova">Nova operação</a>
  </div>
</header>
<main class="app-main"><router-outlet></router-outlet></main>
<footer class="app-footer"><div class="app-container">Controle simples e transparente da sua carteira.</div></footer>
```

- [ ] **Step 5: Criar tokens e componentes compartilhados**

Em `src/styles.css`, importar o Bootstrap já instalado e definir os tokens, componentes e estados interativos abaixo:

```css
@import 'bootstrap/dist/css/bootstrap.min.css';

:root {
  --color-navy-950: #071a2f;
  --color-navy-900: #0b223d;
  --color-emerald-600: #079669;
  --color-emerald-700: #047857;
  --color-slate-700: #334155;
  --color-slate-500: #64748b;
  --color-surface: #ffffff;
  --color-canvas: #f3f7fa;
  --color-border: #dbe4ea;
  --color-danger: #b42318;
  --radius-sm: .65rem;
  --radius-lg: 1rem;
  --shadow-card: 0 14px 35px rgba(7, 26, 47, .08);
}

body { margin: 0; color: var(--color-navy-950); background: var(--color-canvas); }
.app-container { width: min(1180px, calc(100% - 2rem)); margin-inline: auto; }
.page-shell { width: min(1180px, calc(100% - 2rem)); margin: 0 auto; padding: 2.25rem 0 3.5rem; }
.surface-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.numeric { font-variant-numeric: tabular-nums; }
.btn-finance-primary, .btn-finance-secondary {
  align-items: center; border: 1px solid transparent; border-radius: var(--radius-sm);
  display: inline-flex; font-weight: 700; justify-content: center; min-height: 2.75rem;
  padding: .7rem 1rem; text-decoration: none; transition: background-color .2s ease, transform .2s ease;
}
.btn-finance-primary { color: #fff; background: var(--color-emerald-600); }
.btn-finance-primary:hover { color: #fff; background: var(--color-emerald-700); transform: translateY(-1px); }
.btn-finance-secondary { color: var(--color-navy-900); background: #fff; border-color: var(--color-border); }
:where(a, button, input, select):focus-visible { outline: 3px solid rgba(7, 150, 105, .35); outline-offset: 2px; }
:where(button, .btn-finance-primary, .btn-finance-secondary):disabled { cursor: not-allowed; opacity: .6; transform: none; }
```

- [ ] **Step 6: Estilizar cabeçalho, navegação compacta e rodapé**

Em `app.component.css`, manter o fundo azul-marinho, indicador ativo verde, wrapping previsível em telas estreitas e `min-height` para o conteúdo. Em `@media (max-width: 760px)`, distribuir marca e botão na primeira linha e fazer a navegação ocupar uma segunda linha rolável, sem esconder links.

- [ ] **Step 7: Executar o teste do shell e confirmar aprovação**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/app.component.spec.ts`

Expected: PASS para criação, roteamento da home e navegação acessível.

- [ ] **Step 8: Commit do shell**

```bash
git add src/styles.css src/app/app.component.ts src/app/app.component.html src/app/app.component.css src/app/app.component.spec.ts
git commit -m "feat: add financial application shell"
```

### Task 2: Home como painel de entrada da carteira

**Files:**
- Modify: `src/app/pages/home/home.component.spec.ts`
- Modify: `src/app/pages/home/home.component.html`
- Modify: `src/app/pages/home/home.component.css`

**Interfaces:**
- Consumes: classes globais da Task 1 e `RouterLink` já importado em `HomeComponent`.
- Produces: `.hero-panel`, três `.domain-card` e seis `.quick-action`, sem métricas simuladas.

- [ ] **Step 1: Atualizar os testes de hierarquia e destinos**

Manter o teste dos seis destinos e substituir o contrato do título por:

```ts
it('apresenta a carteira sem inventar indicadores financeiros', () => {
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('h1')?.textContent).toContain('Sua carteira, organizada');
  expect(host.querySelectorAll('.domain-card').length).toBe(3);
  expect(host.querySelectorAll('.quick-action').length).toBe(6);
  expect(host.textContent).not.toContain('Rentabilidade');
  expect(host.textContent).not.toContain('Patrimônio');
});
```

- [ ] **Step 2: Executar a spec da home e confirmar a falha**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/home/home.component.spec.ts`

Expected: FAIL pela ausência do novo título e dos cards semânticos.

- [ ] **Step 3: Implementar a nova composição da home**

Organizar `home.component.html` com a seguinte hierarquia:

```html
<main class="page-shell home-page">
  <section class="hero-panel" aria-labelledby="home-title">
    <p class="eyebrow">GESTÃO DE INVESTIMENTOS</p>
    <h1 id="home-title">Sua carteira, organizada em um só lugar</h1>
    <p>Cadastre ativos, acompanhe cotações e registre compras e vendas com clareza.</p>
    <div class="hero-actions">
      <a class="btn-finance-primary" routerLink="/operacoes/nova">Registrar operação</a>
      <a class="btn-finance-secondary" routerLink="/acoes">Ver ações</a>
    </div>
  </section>
  <section aria-labelledby="recursos-title">
    <div class="section-heading"><p class="eyebrow">CARTEIRA</p><h2 id="recursos-title">Comece por aqui</h2></div>
    <div class="domain-grid">
      <article class="domain-card domain-card--broker">
        <p class="domain-card__index">01</p><h3>Corretoras</h3>
        <p>Mantenha os dados das instituições usadas na sua carteira.</p>
        <a routerLink="/corretoras">Ver corretoras</a>
      </article>
      <article class="domain-card domain-card--asset">
        <p class="domain-card__index">02</p><h3>Ações</h3>
        <p>Organize seus ativos e consulte a cotação mais recente.</p>
        <a routerLink="/acoes">Ver ações</a>
      </article>
      <article class="domain-card domain-card--trade">
        <p class="domain-card__index">03</p><h3>Operações</h3>
        <p>Registre compras e vendas e consulte o histórico.</p>
        <a routerLink="/operacoes">Ver operações</a>
      </article>
    </div>
  </section>
  <section class="quick-panel surface-card" aria-labelledby="atalhos-title">
    <h2 id="atalhos-title">Acessos rápidos</h2>
    <div class="quick-grid">
      <a class="quick-action" routerLink="/corretoras/nova"><strong>Cadastrar corretora</strong><span>Adicionar uma instituição</span></a>
      <a class="quick-action" routerLink="/corretoras"><strong>Listar corretoras</strong><span>Consultar instituições</span></a>
      <a class="quick-action" routerLink="/acoes/nova"><strong>Cadastrar ação</strong><span>Vincular um novo ativo</span></a>
      <a class="quick-action" routerLink="/acoes"><strong>Listar ações</strong><span>Consultar ativos e cotações</span></a>
      <a class="quick-action" routerLink="/operacoes/nova"><strong>Nova operação</strong><span>Registrar compra ou venda</span></a>
      <a class="quick-action" routerLink="/operacoes"><strong>Histórico</strong><span>Consultar movimentações</span></a>
    </div>
  </section>
</main>
```

Cada `domain-card` deve explicar o domínio e oferecer links reais de listar/cadastrar; cada `quick-action` deve conter título e descrição curta. A ordem dos seis atalhos continua: `/corretoras/nova`, `/corretoras`, `/acoes/nova`, `/acoes`, `/operacoes/nova`, `/operacoes`.

- [ ] **Step 4: Estilizar a home sem dados fictícios**

Em `home.component.css`, criar grid de três colunas no desktop, duas no tablet e uma no celular; aplicar gradiente discreto no hero e acentos diferentes por domínio usando classes modificadoras. Nenhum número de saldo, rentabilidade ou patrimônio deve aparecer.

- [ ] **Step 5: Executar a spec da home**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/home/home.component.spec.ts`

Expected: PASS com os seis destinos e três domínios.

- [ ] **Step 6: Commit da home**

```bash
git add src/app/pages/home/home.component.html src/app/pages/home/home.component.css src/app/pages/home/home.component.spec.ts
git commit -m "feat: redesign portfolio home dashboard"
```

### Task 3: Formulários financeiros consistentes

**Files:**
- Modify: `src/app/pages/corretora-form/corretora-form.component.spec.ts`
- Modify: `src/app/pages/corretora-form/corretora-form.component.html`
- Modify: `src/app/pages/acao-form/acao-form.component.spec.ts`
- Modify: `src/app/pages/acao-form/acao-form.component.html`
- Modify: `src/app/pages/operacao-form/operacao-form.component.spec.ts`
- Modify: `src/app/pages/operacao-form/operacao-form.component.html`

**Interfaces:**
- Consumes: os mesmos `FormGroup`, handlers, estados e links já presentes nos três componentes.
- Produces: `nav.breadcrumb-nav`, `.form-panel`, `.finance-form`, `.field-error`, `.form-actions` e `.result-card` em cada fluxo.

- [ ] **Step 1: Adicionar um contrato visual a cada spec de formulário**

Adicionar, com o nome do componente correspondente, este teste às três specs:

```ts
it('renderiza o fluxo em um painel financeiro acessível', () => {
  fixture.detectChanges();
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('main.page-shell')).not.toBeNull();
  expect(host.querySelector('nav[aria-label="Navegação estrutural"]')).not.toBeNull();
  expect(host.querySelector('form.finance-form')).not.toBeNull();
  expect(host.querySelector('.form-actions')).not.toBeNull();
});
```

- [ ] **Step 2: Executar somente as três specs e confirmar a falha estrutural**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-form/corretora-form.component.spec.ts --include=src/app/pages/acao-form/acao-form.component.spec.ts --include=src/app/pages/operacao-form/operacao-form.component.spec.ts`

Expected: FAIL porque as classes e breadcrumbs ainda não existem; os testes funcionais anteriores continuam compilando.

- [ ] **Step 3: Reestruturar o cadastro de corretora**

Em `corretora-form.component.html`, envolver o conteúdo em `main.page-shell`, adicionar breadcrumb `Início / Nova corretora`, título e texto auxiliar, e colocar o formulário dentro de `section.form-panel.surface-card`. Manter `formGroup`, `ngSubmit`, ids `cnpj` e `cep`, mensagens server-side, estados e `state.data`. Usar:

```html
<div class="form-actions">
  <button class="btn-finance-primary" type="submit" [disabled]="state.loading">
    {{ state.loading ? 'Salvando…' : 'Salvar corretora' }}
  </button>
  <a routerLink="/corretoras" class="btn-finance-secondary">Cancelar</a>
</div>
```

- [ ] **Step 4: Reestruturar o cadastro de ação**

Em `acao-form.component.html`, usar breadcrumb `Início / Ações / Nova ação`, painel de formulário e texto que explique ticker e vínculo com corretora. Manter os controles `ticker` e `corretoraId`, carregamento/retry de corretoras, mensagens server-side e cartão de resultado. O botão final deve exibir `Salvar ação` quando livre e preservar o bloqueio `state.loading || corretorasState.loading`.

- [ ] **Step 5: Reestruturar a nova operação**

Em `operacao-form.component.html`, usar breadcrumb `Início / Operações / Nova operação`, um painel com os controles `acaoId`, `quantidade`, `tipoOperacao` e o campo condicional `precoVenda`. Destacar Compra e Venda como opções textuais, manter o preço somente para venda e renderizar o resultado em `.result-card` com tipo, ticker, quantidade, total e data já fornecidos pelo backend.

- [ ] **Step 6: Uniformizar erros, ajuda e resultados no CSS global**

Adicionar a `src/styles.css` regras para `.breadcrumb-nav`, `.form-panel`, `.finance-form`, `.form-grid`, `.field-group`, `.field-help`, `.field-error`, `.form-actions` e `.result-card`. Aplicar `aria-describedby` nos inputs com ajuda/erro estável e manter `role="alert"` ou `role="status"` nos feedbacks assíncronos.

- [ ] **Step 7: Executar as specs dos formulários**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-form/corretora-form.component.spec.ts --include=src/app/pages/acao-form/acao-form.component.spec.ts --include=src/app/pages/operacao-form/operacao-form.component.spec.ts`

Expected: PASS para estrutura e para todas as validações, chamadas e bloqueios existentes.

- [ ] **Step 8: Commit dos formulários**

```bash
git add src/styles.css src/app/pages/corretora-form src/app/pages/acao-form src/app/pages/operacao-form
git commit -m "feat: style portfolio forms"
```

### Task 4: Listagens e consultas com linguagem de mercado

**Files:**
- Modify: `src/app/pages/corretora-list/corretora-list.component.spec.ts`
- Modify: `src/app/pages/corretora-list/corretora-list.component.html`
- Modify: `src/app/pages/acao-list/acao-list.component.spec.ts`
- Modify: `src/app/pages/acao-list/acao-list.component.html`
- Modify: `src/app/pages/operacao-list/operacao-list.component.spec.ts`
- Modify: `src/app/pages/operacao-list/operacao-list.component.html`
- Modify: `src/styles.css`

**Interfaces:**
- Consumes: `listState`, `searchState`, `detailState`, `acoesState`, `updating`, `updateErrors`, `filtroAtivo` e handlers atuais.
- Produces: `.list-toolbar`, `.filter-panel`, `.status-panel`, `.table-panel`, `.finance-table`, `.ticker-badge`, `.trade-badge--buy`, `.trade-badge--sell`, `.value-positive` e `.value-negative`.

- [ ] **Step 1: Adicionar contratos estruturais às três specs**

Adicionar este teste às specs de corretoras e ações:

```ts
it('organiza consulta e resultados como uma listagem financeira', () => {
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('main.page-shell')).not.toBeNull();
  expect(host.querySelector('section.filter-panel')).not.toBeNull();
  expect(host.querySelector('a.btn-finance-primary')).not.toBeNull();
});
```

Adicionar à spec de operações:

```ts
it('expõe filtros textuais e tabela financeira', () => {
  component.listState.data = [op];
  fixture.detectChanges();
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('[aria-label="Filtros de operações"]')).not.toBeNull();
  expect(host.querySelector('table.finance-table')).not.toBeNull();
  expect(host.querySelector('.trade-badge')?.textContent).toContain('COMPRA');
});
```

- [ ] **Step 2: Executar as specs de listagem e confirmar a falha**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-list/corretora-list.component.spec.ts --include=src/app/pages/acao-list/acao-list.component.spec.ts --include=src/app/pages/operacao-list/operacao-list.component.spec.ts`

Expected: FAIL pela ausência das novas classes e badges.

- [ ] **Step 3: Redesenhar a listagem de corretoras**

Em `corretora-list.component.html`, criar `main.page-shell`, breadcrumb, `.list-toolbar`, ação `Nova corretora`, `section.filter-panel.surface-card` e `section.table-panel.surface-card`. Preservar consulta ID/CNPJ e todos os estados. Na tabela, aplicar `.finance-table`; exibir ID em `.numeric`, razão social como célula principal, CNPJ monoespaçado/tabular e localidade textual.

- [ ] **Step 4: Redesenhar a listagem de ações**

Em `acao-list.component.html`, usar a mesma hierarquia com ação `Nova ação`. Exibir cada ticker em `.ticker-badge`, cotação em `.numeric`, data em texto secundário e botão de atualização com o loading individual de `updating`. Manter pesquisa ID/ticker, resultado direto, erros por ação e fallback `Indisponível`/`Não atualizada`.

- [ ] **Step 5: Redesenhar a listagem de operações**

Em `operacao-list.component.html`, manter os filtros Todas/Compras/Vendas, histórico por ação e consulta por ID em painéis distintos. Aplicar badges com classe condicional:

```html
<span class="trade-badge"
      [class.trade-badge--buy]="operacao.tipoOperacao === 'COMPRA'"
      [class.trade-badge--sell]="operacao.tipoOperacao === 'VENDA'">
  {{ operacao.tipoOperacao }}
</span>
```

Aplicar `.numeric` a quantidade e valores. Em lucro/prejuízo não nulo, aplicar `[class.value-positive]="operacao.lucroPrejuizo > 0"` e `[class.value-negative]="operacao.lucroPrejuizo < 0"`, mantendo o valor escrito e o marcador `Não informado`.

- [ ] **Step 6: Estilizar filtros, estados e tabelas responsivas**

Adicionar a `src/styles.css` regras de borda e elevação para painéis, cabeçalho escuro suave na tabela, linhas com hover discreto, badges textuais, números tabulares e rolagem horizontal. Em telas até `760px`, permitir quebra do toolbar e manter `min-width` da tabela dentro de `.table-responsive`.

- [ ] **Step 7: Executar as specs das listagens**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-list/corretora-list.component.spec.ts --include=src/app/pages/acao-list/acao-list.component.spec.ts --include=src/app/pages/operacao-list/operacao-list.component.spec.ts`

Expected: PASS para estrutura, filtros, endpoints, retry, estados e valores nulos.

- [ ] **Step 8: Commit das listagens**

```bash
git add src/styles.css src/app/pages/corretora-list src/app/pages/acao-list src/app/pages/operacao-list
git commit -m "feat: style portfolio listings"
```

### Task 5: Responsividade, conteúdo e verificação final

**Files:**
- Modify: `src/styles.css`
- Modify: `src/app/app.component.css`
- Modify: user-facing templates/specs only where corrupted Portuguese text is confirmed.

**Interfaces:**
- Consumes: todas as páginas estilizadas nas Tasks 1–4.
- Produces: frontend compilável, testes completos aprovados e textos em UTF-8 legíveis.

- [ ] **Step 1: Revisar texto e estados visíveis**

Executar uma busca por sequências típicas de codificação corrompida:

```powershell
Get-ChildItem src -Recurse -File | Select-String -Pattern 'Ã.|Â.|â€|�'
```

Expected: nenhuma ocorrência. Se houver, corrigir somente strings e HTML visíveis para português correto, mantendo nomes de propriedades, endpoints e valores de enum como `COMPRA` e `VENDA`.

- [ ] **Step 2: Completar breakpoints e preferências de movimento**

Garantir em `src/styles.css` e `app.component.css`:

```css
@media (max-width: 760px) {
  .page-shell { width: min(100% - 1rem, 1180px); padding-top: 1.25rem; }
  .list-toolbar, .form-actions { align-items: stretch; flex-direction: column; }
  .list-toolbar .btn-finance-primary, .form-actions > * { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
  }
}
```

- [ ] **Step 3: Executar toda a suíte do frontend**

Run: `npm.cmd test -- --watch=false --browsers=ChromeHeadless`

Expected: PASS em todos os testes, sem falha, timeout ou navegador desconectado.

- [ ] **Step 4: Gerar o build de produção**

Run: `npm.cmd run build`

Expected: exit code 0. Avisos existentes de budget devem ser registrados; erros de template, CSS ou TypeScript devem ser corrigidos.

- [ ] **Step 5: Verificar whitespace e arquivos alterados**

Run: `git diff --check`

Expected: nenhuma linha com whitespace inválido.

Run: `git status --short`

Expected: nenhum arquivo do repositório backend e nenhuma dependência nova; somente arquivos frontend/documentação planejados e alterações anteriores já conhecidas.

- [ ] **Step 6: Fazer o commit final de acabamento quando houver mudanças após os commits anteriores**

```bash
git add src/styles.css src/app/app.component.css src/app/app.component.spec.ts src/app/pages
git commit -m "fix: polish responsive portfolio interface"
```

- [ ] **Step 7: Registrar evidências de conclusão**

No handoff final, informar contagem dos testes aprovados, resultado do build, avisos não bloqueantes, hash dos commits criados e confirmar explicitamente que somente `gestao-acao-front` foi modificado durante o redesign.
