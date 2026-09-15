# Nexo Capital Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o frontend institucional da Nexo Capital, com a carteira em uma rota própria e todas as páginas atuais mais sóbrias, consistentes, responsivas e acessíveis.

**Architecture:** Manter os componentes standalone e os serviços HTTP existentes. A nova `CarteiraComponent` consumirá exclusivamente `OperacaoService.resumirCarteira()`, enquanto `OperacaoListComponent` ficará restrita ao extrato e às consultas; o shell, os tokens e os padrões visuais continuarão centralizados em `app.component.*` e `src/styles.css`.

**Tech Stack:** Angular 19.2, TypeScript 5.7, RxJS 7.8, Bootstrap 5.3, Jasmine/Karma e CSS responsivo.

**Spec:** `docs/superpowers/specs/2026-09-14-nexo-capital-frontend-design.md`

## Global Constraints

- A marca exibida deve ser **Nexo Capital**.
- A navegação principal deve permanecer horizontal e conter, nesta ordem: Visão geral, Carteira, Corretoras, Ações e Operações.
- A rota nova deve ser exatamente `/carteira`; as rotas de cadastro existentes devem ser preservadas.
- Não criar endpoints, indicadores financeiros, autenticação, edição ou exclusão de registros.
- Valores consolidados da carteira devem ser apresentados em BRL; operações devem preservar a moeda local de `operacao.acao.moeda`.
- Reutilizar `OperacaoService.resumirCarteira()` sem alterar o contrato `ResumoCarteira`.
- Manter `role="status"`, `aria-live`, `role="alert"`, foco visível, rótulos e `ariaCurrentWhenActive="page"` conforme o estado apresentado.
- Não adicionar biblioteca visual ou de ícones; usar Angular, Bootstrap e CSS já instalados.
- Preservar as alterações locais existentes. Antes de cada commit, executar `git diff --cached --name-only` e nunca incluir `.agents/`, `.codex-ng-serve*.log`, `.superpowers/brainstorm/`, `graphify-out/` ou `openspec/`.

## File Structure

- `src/app/app.component.{ts,html,css}`: identidade Nexo Capital, cabeçalho, navegação horizontal e rodapé.
- `src/styles.css`: tokens e padrões compartilhados de superfícies, métricas, tabelas, filtros, formulários e estados.
- `src/app/app.routes.ts`: registra `/carteira` sem alterar as rotas existentes.
- `src/app/pages/carteira/carteira.component.{ts,html,css,spec.ts}`: nova página isolada de posição consolidada.
- `src/app/pages/home/home.component.{ts,html,css,spec.ts}` e `home-dashboard.css`: resumo executivo, composição relevante e movimentações recentes.
- `src/app/pages/operacao-list/operacao-list.component.{ts,html,css,spec.ts}`: filtros, consulta e extrato, sem resumo de carteira.
- `src/app/pages/corretora-list/*` e `src/app/pages/corretora-form/*`: apresentação institucional do domínio de corretoras.
- `src/app/pages/acao-list/*` e `src/app/pages/acao-form/*`: apresentação institucional de ativos, mercado, moeda e cotação.
- `src/app/pages/operacao-form/*`: contexto do ativo selecionado e moeda local no registro de compra/venda.

---

### Task 1: Aplicar identidade Nexo Capital e sistema visual compartilhado

**Files:**
- Modify: `src/app/app.component.ts:10-12`
- Modify: `src/app/app.component.html:1-18`
- Modify: `src/app/app.component.css:1-129`
- Modify: `src/styles.css:1-167`
- Test: `src/app/app.component.spec.ts:63-220`

**Interfaces:**
- Consumes: `RouterLink`, `RouterLinkActive`, `RouterOutlet` já importados por `AppComponent`.
- Produces: tokens CSS `--color-navy-*`, `--color-emerald-*`, `--color-canvas`, `--color-border`, `--radius-*`, `--shadow-card` e classes compartilhadas usadas nas tarefas seguintes.

- [ ] **Step 1: Registrar o estado atual antes de tocar arquivos já modificados**

Run:

```powershell
git status --short
git diff -- src/app/app.component.spec.ts src/styles.css
```

Expected: as mudanças locais conhecidas permanecem visíveis; nenhum arquivo auxiliar deve ser preparado para commit.

- [ ] **Step 2: Escrever testes falhos para a marca e os tokens institucionais**

Acrescentar a `app.component.spec.ts`:

```typescript
it('apresenta a identidade institucional Nexo Capital', () => {
  const fixture = TestBed.createComponent(AppComponent);
  fixture.detectChanges();
  const host = fixture.nativeElement as HTMLElement;

  expect(fixture.componentInstance.title).toBe('Nexo Capital');
  expect(host.querySelector('.brand')?.getAttribute('aria-label'))
    .toBe('Nexo Capital — visão geral');
  expect(host.querySelector('.brand__mark')?.textContent?.trim()).toBe('NC');
  expect(host.querySelector('.brand__name')?.textContent).toContain('Nexo Capital');
});

it('usa superfícies e raios contidos no sistema institucional', () => {
  const tokens = getComputedStyle(document.documentElement);
  expect(tokens.getPropertyValue('--radius-sm').trim()).toBe('.375rem');
  expect(tokens.getPropertyValue('--radius-lg').trim()).toBe('.625rem');
  expect(tokens.getPropertyValue('--shadow-card').trim())
    .toBe('0 6px 18px rgba(7, 26, 47, .06)');
});
```

- [ ] **Step 3: Executar o teste e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/app.component.spec.ts --progress=false
```

Expected: FAIL porque o título ainda é `Carteira Ações`, a marca é `CA` e os raios/sombra ainda usam os valores antigos.

- [ ] **Step 4: Implementar a marca e os tokens mínimos**

Alterar `AppComponent.title` para:

```typescript
title = 'Nexo Capital';
```

Substituir somente o bloco da marca em `app.component.html` por:

```html
<a class="brand" routerLink="/" aria-label="Nexo Capital — visão geral">
  <span class="brand__mark" aria-hidden="true">NC</span>
  <span class="brand__copy">
    <strong class="brand__name">Nexo Capital</strong>
    <small>Gestão de investimentos</small>
  </span>
</a>
```

Atualizar os tokens e os padrões base em `styles.css`:

```css
:root {
  --color-navy-950: #071a2f;
  --color-navy-900: #0b223d;
  --color-navy-800: #123250;
  --color-emerald-600: #07845f;
  --color-emerald-700: #046c50;
  --color-emerald-800: #03563f;
  --color-slate-700: #334155;
  --color-slate-500: #64748b;
  --color-surface: #ffffff;
  --color-canvas: #f4f6f8;
  --color-border: #d8e0e7;
  --color-danger: #b42318;
  --radius-sm: .375rem;
  --radius-lg: .625rem;
  --shadow-card: 0 6px 18px rgba(7, 26, 47, .06);
}

body {
  margin: 0;
  color: var(--color-navy-950);
  background: var(--color-canvas);
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.surface-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
```

Em `app.component.css`, manter a navegação horizontal e reduzir o caráter decorativo da marca: `brand__mark` com raio `var(--radius-sm)`, cabeçalho com borda inferior sutil e links ativos com a borda verde existente. Não remover o `overflow-x: auto` do breakpoint móvel.

- [ ] **Step 5: Executar o teste direcionado**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/app.component.spec.ts --progress=false
```

Expected: PASS, incluindo contraste AA, foco e preferência por movimento reduzido.

- [ ] **Step 6: Commitar somente o shell e o sistema visual**

```powershell
git add -- src/app/app.component.ts src/app/app.component.html src/app/app.component.css src/app/app.component.spec.ts src/styles.css
git diff --cached --check
git diff --cached --name-only
git commit -m "style: aplicar identidade institucional da Nexo Capital"
```

---

### Task 2: Criar a página Carteira, sua rota e seu destino no menu

**Files:**
- Create: `src/app/pages/carteira/carteira.component.ts`
- Create: `src/app/pages/carteira/carteira.component.html`
- Create: `src/app/pages/carteira/carteira.component.css`
- Create: `src/app/pages/carteira/carteira.component.spec.ts`
- Modify: `src/app/app.routes.ts:1-22`
- Modify: `src/app/app.component.html:7-14`
- Modify: `src/app/app.component.spec.ts:69-91`

**Interfaces:**
- Consumes: `OperacaoService.resumirCarteira(): Observable<ResumoCarteira>` e `RequestState<ResumoCarteira>`.
- Produces: `CarteiraComponent`, rota `/carteira`, getter `resultadoCarteira: number` e destino de navegação “Carteira”.

- [ ] **Step 1: Criar o teste falho da nova página**

Criar `carteira.component.spec.ts` com um spy de `OperacaoService` e os casos essenciais:

```typescript
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';

import { ApiError } from '../../core/api/api-error.interceptor';
import { ResumoCarteira } from '../../core/api/api.models';
import { OperacaoService } from '../../services/operacao.service';
import { CarteiraComponent } from './carteira.component';

registerLocaleData(localePt, 'pt-BR');

const resumo: ResumoCarteira = {
  posicoes: [
    { acaoId: 7, ticker: 'AAPL', quantidadeAtual: 2, precoMedio: 525, custoTotal: 1050, cotacaoAtual: 550, valorAtual: 1100 }
  ],
  quantidadeTotal: 2,
  precoMedioCarteira: 525,
  custoTotalCarteira: 1050,
  valorAtualCarteira: 1100
};

describe('CarteiraComponent', () => {
  let component: CarteiraComponent;
  let fixture: ComponentFixture<CarteiraComponent>;
  let service: jasmine.SpyObj<OperacaoService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<OperacaoService>('OperacaoService', ['resumirCarteira']);
    service.resumirCarteira.and.returnValue(of(resumo));
    await TestBed.configureTestingModule({
      imports: [CarteiraComponent],
      providers: [
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: OperacaoService, useValue: service }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CarteiraComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

it('carrega e apresenta a carteira consolidada em BRL', () => {
  expect(service.resumirCarteira).toHaveBeenCalledTimes(1);
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('[data-testid="valor-atual"]')?.textContent).toContain('1.100,00');
  expect(host.querySelector('[data-testid="resultado"]')?.textContent).toContain('50,00');
  expect(host.querySelector('[data-testid="numero-posicoes"]')?.textContent).toContain('1');
  expect(host.querySelector('[data-testid="posicao-AAPL"]')?.textContent).toContain('AAPL');
  expect(host.textContent).toContain('convertidos para BRL pela cotação atual');
});

it('expõe carregamento, erro com retry e estado vazio', () => {
  const pending = new Subject<ResumoCarteira>();
  service.resumirCarteira.and.returnValue(pending);
  component.carregarCarteira();
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('[role="status"]')).not.toBeNull();

  pending.error(new ApiError(503, 'Indisponível', 'Carteira indisponível.'));
  fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent)
    .toContain('Carteira indisponível.');

  service.resumirCarteira.and.returnValue(of({ ...resumo, posicoes: [], quantidadeTotal: 0 }));
  (fixture.nativeElement.querySelector('[data-testid="carteira-retry"]') as HTMLButtonElement).click();
  fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('Nenhuma ação em posse');
});
});
```

- [ ] **Step 2: Executar o teste e confirmar que o componente ainda não existe**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/carteira/carteira.component.spec.ts --progress=false
```

Expected: FAIL de compilação porque `CarteiraComponent` ainda não foi criado.

- [ ] **Step 3: Implementar o estado e os cálculos da CarteiraComponent**

Criar `carteira.component.ts`:

```typescript
import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/api/api-error.interceptor';
import { ResumoCarteira } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { OperacaoService } from '../../services/operacao.service';

@Component({
  selector: 'app-carteira',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './carteira.component.html',
  styleUrl: './carteira.component.css'
})
export class CarteiraComponent implements OnInit {
  private readonly operacaoService = inject(OperacaoService);
  carteiraState: RequestState<ResumoCarteira> = initialRequestState<ResumoCarteira>();

  ngOnInit(): void {
    this.carregarCarteira();
  }

  get resultadoCarteira(): number {
    const carteira = this.carteiraState.data;
    return carteira ? carteira.valorAtualCarteira - carteira.custoTotalCarteira : 0;
  }

  carregarCarteira(): void {
    this.carteiraState = toLoadingState(this.carteiraState);
    this.operacaoService.resumirCarteira().subscribe({
      next: data => this.carteiraState = toSuccessState(this.carteiraState, data),
      error: (error: ApiError) => this.carteiraState = toErrorState(this.carteiraState, error)
    });
  }
}
```

- [ ] **Step 4: Implementar o template e o CSS da carteira**

Em `carteira.component.html`, criar: breadcrumb; cabeçalho com “Carteira consolidada” e botão Atualizar; `role="status"` no carregamento; `role="alert"` com botão `data-testid="carteira-retry"`; cinco métricas com `valor-atual`, `custo-total`, `resultado`, `quantidade-total` e `numero-posicoes`; nota cambial; estado vazio; e tabela com `data-testid="posicao-{{ ticker }}"` contendo quantidade, preço médio, custo, cotação e valor atual.

Usar obrigatoriamente estas expressões monetárias:

```html
{{ carteira.valorAtualCarteira | currency:'BRL' }}
{{ carteira.custoTotalCarteira | currency:'BRL' }}
{{ resultadoCarteira | currency:'BRL' }}
{{ posicao.precoMedio | currency:'BRL' }}
{{ posicao.custoTotal | currency:'BRL' }}
{{ posicao.cotacaoAtual | currency:'BRL' }}
{{ posicao.valorAtual | currency:'BRL' }}
```

Em `carteira.component.css`, definir `.portfolio-metrics` com cinco colunas, `.currency-note` com borda esquerda verde, tabela mínima de `760px`, duas colunas abaixo de `900px` e uma abaixo de `560px`.

- [ ] **Step 5: Registrar a rota e o quinto destino principal**

Adicionar a importação e a rota em `app.routes.ts`:

```typescript
import { CarteiraComponent } from './pages/carteira/carteira.component';

{ path: 'carteira', component: CarteiraComponent },
```

Adicionar entre Visão geral e Corretoras em `app.component.html`:

```html
<a routerLink="/carteira" routerLinkActive="is-active" ariaCurrentWhenActive="page">Carteira</a>
```

Atualizar o teste de destinos para esperar:

```typescript
expect(destinations).toEqual(['/', '/carteira', '/corretoras', '/acoes', '/operacoes']);
```

E acrescentar um teste que navega para `/carteira` e encontra o `h1` “Carteira consolidada”.

- [ ] **Step 6: Executar os testes da página e do roteamento**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/carteira/carteira.component.spec.ts --include=src/app/app.component.spec.ts --progress=false
```

Expected: PASS; `/carteira` renderiza, o menu contém cinco destinos e a moeda da tabela é BRL.

- [ ] **Step 7: Commitar a página Carteira**

```powershell
git add -- src/app/app.routes.ts src/app/app.component.html src/app/app.component.spec.ts src/app/pages/carteira
git diff --cached --check
git diff --cached --name-only
git commit -m "feat: separar carteira em pagina propria"
```

---

### Task 3: Remover a carteira de Operações e consolidar o extrato

**Files:**
- Modify: `src/app/pages/operacao-list/operacao-list.component.ts:1-130`
- Modify: `src/app/pages/operacao-list/operacao-list.component.html:1-248`
- Modify: `src/app/pages/operacao-list/operacao-list.component.css:1-53`
- Modify: `src/app/pages/operacao-list/operacao-list.component.spec.ts:1-118`

**Interfaces:**
- Consumes: métodos `listar`, `listarCompras`, `listarVendas`, `listarHistorico` e `buscarPorId` de `OperacaoService`, mais `AcaoService.listar()`.
- Produces: `OperacaoListComponent` sem `carteiraState` e sem chamada a `resumirCarteira()`.

- [ ] **Step 1: Alterar o teste para exigir a separação**

Remover `ResumoCarteira` e a constante `resumo` do spec. Manter `resumirCarteira` no spy apenas para provar que não é usado e substituir o teste final por:

```typescript
it('mantém operações independente do resumo da carteira', () => {
  expect(service.resumirCarteira).not.toHaveBeenCalled();
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('.portfolio-panel')).toBeNull();
  expect(host.querySelector('[data-testid="quantidade-total"]')).toBeNull();
  expect(host.querySelector('[aria-label="Filtros de operações"]')).not.toBeNull();
});
```

- [ ] **Step 2: Executar o teste e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/operacao-list/operacao-list.component.spec.ts --progress=false
```

Expected: FAIL porque `ngOnInit()` ainda chama `resumirCarteira()` e o painel ainda existe.

- [ ] **Step 3: Remover estado, carregamento e markup de carteira**

Em `operacao-list.component.ts`, remover `ResumoCarteira` da importação, `carteiraState`, `carregarCarteira()` e a chamada correspondente em `ngOnInit()`; preservar a proteção `listRequestGeneration` contra respostas antigas.

Em `operacao-list.component.html`, remover integralmente o bloco `.portfolio-panel`. Atualizar a introdução para:

```html
<p class="form-intro">Consulte compras, vendas, movimentações por ativo e detalhes por identificador.</p>
```

Agrupar os dois elementos `section.filter-panel` existentes em um único `div.operation-query-grid`: “Filtrar operações” deve ser o primeiro filho e “Buscar operação por ID” o segundo. Mover os painéis completos, sem alterar formulários, validações ou estados.

- [ ] **Step 4: Substituir o CSS antigo do portfólio pelo layout de consultas**

Substituir `operacao-list.component.css` por:

```css
.operation-query-grid {
  align-items: start;
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1.25fr) minmax(18rem, .75fr);
}

.operation-query-grid .filter-panel {
  margin-bottom: 0;
  min-width: 0;
}

@media (max-width: 900px) {
  .operation-query-grid {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 5: Executar o teste direcionado**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/operacao-list/operacao-list.component.spec.ts --progress=false
```

Expected: PASS; todos os testes de filtros, retry, concorrência e moedas locais continuam passando.

- [ ] **Step 6: Commitar a separação**

```powershell
git add -- src/app/pages/operacao-list
git diff --cached --check
git diff --cached --name-only
git commit -m "refactor: dedicar operacoes ao extrato financeiro"
```

---

### Task 4: Refinar a Visão geral como resumo executivo

**Files:**
- Modify: `src/app/pages/home/home.component.ts:22-68`
- Modify: `src/app/pages/home/home.component.html:1-204`
- Modify: `src/app/pages/home/home.component.css:1-193`
- Modify: `src/app/pages/home/home-dashboard.css:1-43`
- Modify: `src/app/pages/home/home.component.spec.ts:118-180`

**Interfaces:**
- Consumes: `ResumoCarteira`, `OperacaoAcao[]`, `allocationPercent()` e os dois métodos existentes de `OperacaoService`.
- Produces: getter `posicoesRelevantes: PosicaoCarteira[]`, dashboard limitado às cinco maiores posições e links explícitos para `/carteira` e `/operacoes`.

- [ ] **Step 1: Escrever testes falhos para a hierarquia executiva**

Substituir os testes de atalhos/hero por:

```typescript
it('prioriza as cinco maiores posições no resumo executivo', () => {
  component.carteiraState.data = {
    ...carteira,
    posicoes: [
      ...carteira.posicoes,
      { acaoId: 3, ticker: 'ITUB4', quantidadeAtual: 3, precoMedio: 20, custoTotal: 60, cotacaoAtual: 30, valorAtual: 90 },
      { acaoId: 4, ticker: 'BBDC4', quantidadeAtual: 2, precoMedio: 25, custoTotal: 50, cotacaoAtual: 30, valorAtual: 60 },
      { acaoId: 5, ticker: 'WEGE3', quantidadeAtual: 1, precoMedio: 45, custoTotal: 45, cotacaoAtual: 50, valorAtual: 50 },
      { acaoId: 6, ticker: 'ABEV3', quantidadeAtual: 1, precoMedio: 12, custoTotal: 12, cotacaoAtual: 15, valorAtual: 15 }
    ]
  };
  expect(component.posicoesRelevantes.length).toBe(5);
  expect(component.posicoesRelevantes[0].valorAtual)
    .toBeGreaterThanOrEqual(component.posicoesRelevantes[4].valorAtual);
});

it('oferece acesso direto à carteira e ao extrato sem duplicar cadastros', () => {
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('h1')?.textContent).toContain('Visão consolidada');
  expect(host.querySelector('a[href="/carteira"]')).not.toBeNull();
  expect(host.querySelector('a[href="/operacoes"]')).not.toBeNull();
  expect(host.querySelector('.quick-grid')).toBeNull();
  expect(host.querySelector('.domain-grid')).toBeNull();
});
```

- [ ] **Step 2: Executar o teste e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/home/home.component.spec.ts --progress=false
```

Expected: FAIL porque o getter não existe, o título ainda é promocional e os dois blocos redundantes ainda são renderizados.

- [ ] **Step 3: Implementar a seleção de posições relevantes**

Adicionar a `HomeComponent`:

```typescript
get posicoesRelevantes(): PosicaoCarteira[] {
  return [...(this.carteiraState.data?.posicoes ?? [])]
    .sort((first, second) => second.valorAtual - first.valorAtual)
    .slice(0, 5);
}
```

Manter `operacoesRecentes` com limite de cinco e `allocationPercent()` calculado sobre o total integral, não somente sobre as posições exibidas.

- [ ] **Step 4: Reorganizar o template como página executiva**

Trocar o título por “Visão consolidada dos seus investimentos”; manter as quatro métricas; iterar `posicoesRelevantes` no bloco de composição; alterar “Ver carteira” para `/carteira`; manter “Ver histórico” em `/operacoes`; remover integralmente `.domain-grid` e `.quick-panel`; e manter uma ação principal “Registrar operação” e uma secundária “Abrir carteira”.

Os valores consolidados permanecem:

```html
{{ carteira.valorAtualCarteira | currency:'BRL' }}
{{ carteira.custoTotalCarteira | currency:'BRL' }}
{{ resultadoCarteira | currency:'BRL' }}
```

As movimentações recentes permanecem na moeda local:

```html
{{ operacao.valorTotal | currency:(operacao.acao.moeda || 'BRL') }}
```

Em `home.component.css` e `home-dashboard.css`, remover regras sem consumidores após a exclusão dos atalhos, retirar gradientes decorativos e manter os breakpoints de `900px` e `640px` para métricas e painéis.

- [ ] **Step 5: Executar o teste da home**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/home/home.component.spec.ts --progress=false
```

Expected: PASS; quatro KPIs, até cinco posições e até cinco operações recentes são renderizados com as moedas corretas.

- [ ] **Step 6: Commitar a Visão geral**

```powershell
git add -- src/app/pages/home
git diff --cached --check
git diff --cached --name-only
git commit -m "style: transformar visao geral em resumo executivo"
```

---

### Task 5: Padronizar Corretoras e seu cadastro

**Files:**
- Modify: `src/app/pages/corretora-list/corretora-list.component.html:1-129`
- Modify: `src/app/pages/corretora-list/corretora-list.component.css`
- Modify: `src/app/pages/corretora-list/corretora-list.component.spec.ts:110-135`
- Modify: `src/app/pages/corretora-form/corretora-form.component.html:1-91`
- Modify: `src/app/pages/corretora-form/corretora-form.component.css`
- Modify: `src/app/pages/corretora-form/corretora-form.component.spec.ts:75-170`
- Modify: `src/styles.css`

**Interfaces:**
- Consumes: estados e métodos atuais de `CorretoraListComponent` e `CorretoraFormComponent`; nenhum método TypeScript novo.
- Produces: classes visuais `entity-status`, `entity-status--verified` e painéis institucionais coerentes com o sistema global.

- [ ] **Step 1: Escrever testes falhos para a apresentação cadastral**

No spec da lista, renderizar uma corretora validada e verificar:

```typescript
it('destaca o estado cadastral sem alterar os dados da consulta', () => {
  component.listState.data = [corretora];
  fixture.detectChanges();
  const host = fixture.nativeElement as HTMLElement;
  expect(host.querySelector('.query-panel--compact')).not.toBeNull();
  expect(host.querySelector('.entity-status--verified')?.textContent).toContain('Validada na CVM');
  expect(host.querySelector('[data-testid="corretora-count"]')?.textContent).toContain('1 registro');
});
```

No spec do formulário, verificar que `.form-panel` contém `.form-section-title`, que erros continuam associados por `aria-describedby` e que Cancelar aponta para `/corretoras`.

- [ ] **Step 2: Executar os testes e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-list/corretora-list.component.spec.ts --include=src/app/pages/corretora-form/corretora-form.component.spec.ts --progress=false
```

Expected: FAIL porque contagem, selo cadastral e título interno ainda não existem.

- [ ] **Step 3: Refinar lista e formulário sem tocar a lógica de busca**

Adicionar `query-panel--compact` ao painel de consulta. No cabeçalho da tabela, mostrar:

```html
<span class="record-count numeric" data-testid="corretora-count">
  {{ listState.data.length }} {{ listState.data.length === 1 ? 'registro' : 'registros' }}
</span>
```

Adicionar uma coluna “Situação” com:

```html
<span class="entity-status" [class.entity-status--verified]="corretora.validadaNaCvm">
  {{ corretora.validadaNaCvm ? 'Validada na CVM' : (corretora.situacaoCadastral || 'Não informada') }}
</span>
```

No formulário, incluir `<h2 class="form-section-title">Identificação da instituição</h2>` antes do grid; trocar alertas Bootstrap por `status-panel`/`status-panel--error`; preservar IDs, `aria-describedby`, mensagens do servidor e o destino `/corretoras`.

Adicionar a `src/styles.css`:

```css
.table-panel__header {
  align-items: center;
  display: flex;
  gap: 1rem;
  justify-content: space-between;
}

.record-count {
  color: var(--color-slate-500);
  font-size: .8125rem;
  font-weight: 700;
  white-space: nowrap;
}

.form-section-title {
  color: var(--color-navy-900);
  font-size: 1rem;
  font-weight: 800;
  margin: 0;
}

.entity-status {
  background: #eef2f6;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-slate-700);
  display: inline-flex;
  font-size: .75rem;
  font-weight: 800;
  padding: .35rem .65rem;
  white-space: nowrap;
}

.entity-status--verified {
  background: #e4f4ed;
  border-color: #b8dfcf;
  color: var(--color-emerald-800);
}
```

Em `corretora-list.component.css`, limitar `.query-panel--compact .panel-heading` a `42rem`; em `corretora-form.component.css`, não duplicar os estilos globais.

- [ ] **Step 4: Executar os testes direcionados**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/corretora-list/corretora-list.component.spec.ts --include=src/app/pages/corretora-form/corretora-form.component.spec.ts --progress=false
```

Expected: PASS; busca por ID/CNPJ, retry, validação de campos e retorno enriquecido continuam funcionando.

- [ ] **Step 5: Commitar o domínio Corretoras**

```powershell
git add -- src/styles.css src/app/pages/corretora-list src/app/pages/corretora-form
git diff --cached --check
git diff --cached --name-only
git commit -m "style: padronizar experiencia de corretoras"
```

---

### Task 6: Tornar Ações mais legível por mercado e moeda

**Files:**
- Modify: `src/app/pages/acao-list/acao-list.component.html:1-142`
- Modify: `src/app/pages/acao-list/acao-list.component.css`
- Modify: `src/app/pages/acao-list/acao-list.component.spec.ts:81-109`
- Modify: `src/app/pages/acao-form/acao-form.component.html:1-99`
- Modify: `src/app/pages/acao-form/acao-form.component.css`
- Modify: `src/app/pages/acao-form/acao-form.component.spec.ts:52-129`
- Modify: `src/styles.css`

**Interfaces:**
- Consumes: `Acao.mercado`, `Acao.moeda`, `Acao.cotacaoAtual`, `AcaoService.atualizarCotacao()` e os estados atuais.
- Produces: leitura explícita de mercado/moeda e classes compartilhadas `market-badge` e `currency-code`.

- [ ] **Step 1: Escrever testes falhos para mercado, moeda e navegação**

No spec da lista, usar uma ação com `mercado: 'NASDAQ'` e `moeda: 'USD'`:

```typescript
it('separa mercado e moeda da cotação internacional', () => {
  component.listState.data = [{ ...acao, mercado: 'NASDAQ', moeda: 'USD', cotacaoAtual: 190 }];
  fixture.detectChanges();
  const row = fixture.nativeElement.querySelector('tbody tr') as HTMLTableRowElement;
  expect(row.querySelector('.market-badge')?.textContent).toContain('NASDAQ');
  expect(row.querySelector('.currency-code')?.textContent).toContain('USD');
  expect(row.textContent).toContain('US$');
});
```

No spec do formulário, verificar que Cancelar aponta para `/acoes` e que carregamento/erro de corretoras usam `role="status"` e `role="alert"` com o botão de retry institucional.

- [ ] **Step 2: Executar os testes e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/acao-list/acao-list.component.spec.ts --include=src/app/pages/acao-form/acao-form.component.spec.ts --progress=false
```

Expected: FAIL porque mercado e moeda ainda não têm células/classes próprias e Cancelar ainda aponta para `/`.

- [ ] **Step 3: Reorganizar a tabela e o cadastro de Ações**

Na tabela, usar colunas ID, Ativo, Empresa, Mercado, Moeda, Corretora, Cotação, Atualização e Ação. Renderizar:

```html
<td><span class="market-badge">{{ a.mercado || 'Não informado' }}</span></td>
<td><span class="currency-code">{{ a.moeda || 'BRL' }}</span></td>
<td class="numeric">{{ a.cotacaoAtual === null ? 'Indisponível' : (a.cotacaoAtual | currency:(a.moeda || 'BRL')) }}</td>
```

Manter Atualizar cotação como botão secundário, o estado `updating` por ID e `updateErrors[a.id]` junto da linha correspondente.

No formulário, trocar alerts por painéis padronizados, inserir o título interno “Identificação do ativo” e alterar somente o link Cancelar para:

```html
<a routerLink="/acoes" class="btn-finance-secondary">Cancelar</a>
```

Preservar a consulta externa feita pelo backend: o frontend continua enviando somente `ticker` e `corretoraId`.

Adicionar a `src/styles.css`:

```css
.market-badge,
.currency-code {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  display: inline-flex;
  font-size: .75rem;
  font-weight: 800;
  letter-spacing: .04em;
  padding: .3rem .5rem;
  white-space: nowrap;
}

.market-badge {
  background: #eef3f7;
  color: var(--color-navy-900);
}

.currency-code {
  background: #f8fafc;
  color: var(--color-slate-700);
  font-variant-numeric: tabular-nums;
}
```

Em `acao-list.component.css`, definir `.finance-table { min-width: 1040px; }`; em `acao-form.component.css`, não duplicar estilos globais.

- [ ] **Step 4: Executar os testes direcionados**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/acao-list/acao-list.component.spec.ts --include=src/app/pages/acao-form/acao-form.component.spec.ts --progress=false
```

Expected: PASS; busca, atualização de cotação, erro por linha, cadastro e moeda internacional permanecem corretos.

- [ ] **Step 5: Commitar o domínio Ações**

```powershell
git add -- src/styles.css src/app/pages/acao-list src/app/pages/acao-form
git diff --cached --check
git diff --cached --name-only
git commit -m "style: destacar mercado e moeda dos ativos"
```

---

### Task 7: Contextualizar moeda no formulário de Operações e concluir a verificação

**Files:**
- Modify: `src/app/pages/operacao-form/operacao-form.component.ts:13-62`
- Modify: `src/app/pages/operacao-form/operacao-form.component.html:1-153`
- Modify: `src/app/pages/operacao-form/operacao-form.component.css`
- Modify: `src/app/pages/operacao-form/operacao-form.component.spec.ts:48-126`
- Modify: `src/styles.css`

**Interfaces:**
- Consumes: `Acao` selecionada em `acoesState`, `Acao.moeda`, `Acao.mercado`, `Acao.cotacaoAtual`, `OperacaoService.comprar()` e `vender()`.
- Produces: getters `acaoSelecionada: Acao | null` e `moedaSelecionada: string`; o payload HTTP permanece inalterado.

- [ ] **Step 1: Escrever o teste falho do contexto internacional**

Adicionar ao spec uma ação internacional e verificar:

```typescript
it('contextualiza preço e cotação na moeda local do ativo selecionado', () => {
  const internacional = { ...acao, id: 9, ticker: 'AAPL', mercado: 'NASDAQ', moeda: 'USD', cotacaoAtual: 190 };
  component.acoesState.data = [internacional];
  component.form.controls.acaoId.setValue(9);
  fixture.detectChanges();

  const context = fixture.nativeElement.querySelector('.asset-context') as HTMLElement;
  expect(component.acaoSelecionada).toEqual(internacional);
  expect(component.moedaSelecionada).toBe('USD');
  expect(context.textContent).toContain('AAPL');
  expect(context.textContent).toContain('NASDAQ');
  expect(context.textContent).toContain('USD');
  expect(fixture.nativeElement.querySelector('#preco-compra-help')?.textContent)
    .toContain('Valor informado em USD');
});
```

Atualizar o teste de navegação para exigir Cancelar em `/operacoes`.

- [ ] **Step 2: Executar o teste e confirmar a falha**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/operacao-form/operacao-form.component.spec.ts --progress=false
```

Expected: FAIL porque getters, contexto do ativo e texto dinâmico de moeda ainda não existem.

- [ ] **Step 3: Implementar os getters sem mudar a submissão**

Adicionar a `OperacaoFormComponent`:

```typescript
get acaoSelecionada(): Acao | null {
  const acaoId = this.form.controls.acaoId.value;
  return this.acoesState.data?.find(acao => acao.id === acaoId) ?? null;
}

get moedaSelecionada(): string {
  return this.acaoSelecionada?.moeda || 'BRL';
}
```

Não converter valores no frontend e não mudar `comprar(acaoId, quantidade, precoCompra)` ou `vender(acaoId, quantidade, precoVenda)`.

- [ ] **Step 4: Exibir contexto e moeda local no formulário**

Depois do seletor de ação, renderizar quando houver seleção:

```html
@if (acaoSelecionada; as acao) {
  <aside class="asset-context" aria-label="Contexto do ativo selecionado">
    <span class="ticker-badge">{{ acao.ticker }}</span>
    <span>{{ acao.nomeEmpresa || 'Empresa não informada' }}</span>
    <span class="market-badge">{{ acao.mercado || 'Mercado não informado' }}</span>
    <span class="currency-code">{{ moedaSelecionada }}</span>
    <strong class="numeric">
      {{ acao.cotacaoAtual === null ? 'Cotação indisponível' : (acao.cotacaoAtual | currency:moedaSelecionada) }}
    </strong>
  </aside>
}
```

Nos textos auxiliares de compra e venda, começar com `Valor informado em {{ moedaSelecionada }}.` e manter a explicação já existente. Alterar Cancelar para `/operacoes`; substituir alerts de ações/resultado por estados compartilhados; estilizar `.asset-context` como faixa compacta, responsiva e sem sombra adicional.

Usar em `operacao-form.component.css`:

```css
.asset-context {
  align-items: center;
  background: #f7f9fb;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  display: flex;
  flex-wrap: wrap;
  gap: .6rem 1rem;
  grid-column: 1 / -1;
  padding: .85rem 1rem;
}

.asset-context strong {
  margin-left: auto;
}

@media (max-width: 560px) {
  .asset-context {
    align-items: flex-start;
    flex-direction: column;
  }

  .asset-context strong {
    margin-left: 0;
  }
}
```

- [ ] **Step 5: Executar o teste direcionado**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --include=src/app/pages/operacao-form/operacao-form.component.spec.ts --progress=false
```

Expected: PASS; compras e vendas continuam enviando preço e quantidade originais, e USD aparece apenas como contexto/formatação local.

- [ ] **Step 6: Executar a suíte completa, build e validação do diff**

Run:

```powershell
npm.cmd test -- --watch=false --browsers=ChromeHeadless --progress=false
npm.cmd run build
git diff --check
```

Expected: todos os testes Angular passam, o build de produção conclui sem erro e `git diff --check` não encontra whitespace inválido. Avisos de LF/CRLF podem ser registrados, mas não são falha funcional.

- [ ] **Step 7: Fazer auditoria manual de responsividade e acessibilidade**

Com `npm.cmd start`, conferir em larguras de 1440px, 1024px, 768px e 390px:

```text
Cabeçalho: cinco destinos acessíveis; rolagem horizontal em 390px; ação Nova operação visível.
Visão geral: quatro métricas; no máximo cinco posições; operações recentes na moeda local.
Carteira: métricas e tabela em BRL; nota cambial; retry e vazio utilizáveis.
Corretoras/Ações/Operações: filtros não se sobrepõem; tabelas têm rolagem horizontal controlada.
Formulários: foco visível; labels e erros associados; botões desabilitados durante requisições.
```

Expected: nenhuma sobreposição, corte de destino ou conteúdo inacessível; `aria-current="page"` aparece no destino ativo.

- [ ] **Step 8: Commitar o formulário e o acabamento final**

```powershell
git add -- src/styles.css src/app/pages/operacao-form
git diff --cached --check
git diff --cached --name-only
git commit -m "style: contextualizar moeda nas operacoes"
git status --short
```

Expected: o commit contém somente os arquivos listados; diretórios auxiliares continuam fora do índice.
