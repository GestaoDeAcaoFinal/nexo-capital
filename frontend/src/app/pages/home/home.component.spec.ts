import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { OperacaoAcao, ResumoCarteira } from '../../core/api/api.models';
import { OperacaoService } from '../../services/operacao.service';
import { HomeComponent } from './home.component';

registerLocaleData(localePt, 'pt-BR');

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let operacaoService: jasmine.SpyObj<OperacaoService>;

  const carteira: ResumoCarteira = {
    quantidadeTotal: 12,
    precoMedioCarteira: 30,
    custoTotalCarteira: 360,
    valorAtualCarteira: 420,
    posicoes: [
      { acaoId: 1, ticker: 'PETR4', quantidadeAtual: 8, precoMedio: 25, custoTotal: 200, cotacaoAtual: 35, valorAtual: 280 },
      { acaoId: 2, ticker: 'VALE3', quantidadeAtual: 4, precoMedio: 40, custoTotal: 160, cotacaoAtual: 35, valorAtual: 140 }
    ]
  };

  const operacoes: OperacaoAcao[] = [
    {
      id: 2,
      quantidade: 4,
      precoUnitario: 40,
      precoMedio: 40,
      lucroPrejuizo: null,
      dataOperacao: '2026-09-10T10:00:00',
      valorTotal: 160,
      tipoOperacao: 'COMPRA',
      acao: {
        id: 2,
        ticker: 'VALE3',
        nomeEmpresa: 'Vale',
        mercado: 'BR',
        moeda: 'BRL',
        cotacaoAtual: 35,
        dataHoraCotacao: null,
        corretoraRelacionada: {} as OperacaoAcao['acao']['corretoraRelacionada']
      }
    },
    {
      id: 1,
      quantidade: 8,
      precoUnitario: 25,
      precoMedio: 25,
      lucroPrejuizo: null,
      dataOperacao: '2026-09-12T14:30:00',
      valorTotal: 200,
      tipoOperacao: 'COMPRA',
      acao: {
        id: 1,
        ticker: 'PETR4',
        nomeEmpresa: 'Petrobras',
        mercado: 'BR',
        moeda: 'BRL',
        cotacaoAtual: 35,
        dataHoraCotacao: null,
        corretoraRelacionada: {} as OperacaoAcao['acao']['corretoraRelacionada']
      }
    }
  ];

  beforeEach(async () => {
    operacaoService = jasmine.createSpyObj<OperacaoService>('OperacaoService', ['resumirCarteira', 'listar']);
    operacaoService.resumirCarteira.and.returnValue(of(carteira));
    operacaoService.listar.and.returnValue(of(operacoes));

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: OperacaoService, useValue: operacaoService }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('apresenta indicadores financeiros obtidos da API', () => {
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('h1')?.textContent).toContain('Visão consolidada');
    expect(host.querySelector('[data-testid="valor-atual"]')?.textContent).toMatch(/R\$\s*420,00/);
    expect(host.querySelector('[data-testid="custo-total"]')?.textContent).toMatch(/R\$\s*360,00/);
    expect(host.querySelector('[data-testid="resultado"]')?.textContent).toMatch(/R\$\s*60,00/);
    expect(host.querySelector('[data-testid="quantidade-total"]')?.textContent).toContain('12');
  });

  it('mostra a distribuição por ativo e as operações mais recentes', () => {
    const host = fixture.nativeElement as HTMLElement;
    const allocations = Array.from(host.querySelectorAll('.allocation-row'));
    const recentOperations = Array.from(host.querySelectorAll('.recent-operation'));

    expect(allocations.length).toBe(2);
    expect(allocations[0].textContent).toContain('PETR4');
    expect(allocations[0].querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('66.67');
    expect(recentOperations.length).toBe(2);
    expect(recentOperations[0].textContent).toContain('PETR4');
    expect(recentOperations[1].textContent).toContain('VALE3');
  });

  it('renderiza as cinco maiores posições sem reduzir os totais dos KPIs', () => {
    component.carteiraState.data = {
      ...carteira,
      quantidadeTotal: 19,
      custoTotalCarteira: 527,
      valorAtualCarteira: 635,
      posicoes: [
        ...carteira.posicoes,
        { acaoId: 3, ticker: 'ITUB4', quantidadeAtual: 3, precoMedio: 20, custoTotal: 60, cotacaoAtual: 30, valorAtual: 90 },
        { acaoId: 4, ticker: 'BBDC4', quantidadeAtual: 2, precoMedio: 25, custoTotal: 50, cotacaoAtual: 30, valorAtual: 60 },
        { acaoId: 5, ticker: 'WEGE3', quantidadeAtual: 1, precoMedio: 45, custoTotal: 45, cotacaoAtual: 50, valorAtual: 50 },
        { acaoId: 6, ticker: 'ABEV3', quantidadeAtual: 1, precoMedio: 12, custoTotal: 12, cotacaoAtual: 15, valorAtual: 15 }
      ]
    };
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const allocations = Array.from(host.querySelectorAll('.allocation-row'));
    const tickers = allocations.map(row => row.querySelector('.ticker-badge')?.textContent?.trim());

    expect(allocations.length).toBe(5);
    expect(tickers).toEqual(['PETR4', 'VALE3', 'ITUB4', 'BBDC4', 'WEGE3']);
    expect(allocations[0].querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('44.09');
    expect(host.querySelector('[data-testid="valor-atual"]')?.textContent).toMatch(/R\$\s*635,00/);
    expect(host.querySelector('[data-testid="custo-total"]')?.textContent).toMatch(/R\$\s*527,00/);
    expect(host.querySelector('[data-testid="resultado"]')?.textContent).toMatch(/R\$\s*108,00/);
    expect(host.querySelector('[data-testid="quantidade-total"]')?.textContent).toContain('19');
  });

  it('renderiza somente as cinco operações mais recentes em ordem e na moeda local', () => {
    component.operacoesState.data = [
      ...operacoes,
      {
        ...operacoes[0],
        id: 6,
        dataOperacao: '2026-09-15T09:00:00',
        valorTotal: 1234.56,
        acao: {
          ...operacoes[0].acao,
          id: 6,
          ticker: 'MSFT',
          nomeEmpresa: 'Microsoft',
          mercado: 'US',
          moeda: 'USD'
        }
      },
      {
        ...operacoes[0],
        id: 5,
        dataOperacao: '2026-09-14T09:00:00',
        valorTotal: 90,
        acao: { ...operacoes[0].acao, id: 5, ticker: 'ITUB4', nomeEmpresa: 'Itaú' }
      },
      {
        ...operacoes[0],
        id: 4,
        dataOperacao: '2026-09-13T09:00:00',
        valorTotal: 60,
        acao: { ...operacoes[0].acao, id: 4, ticker: 'BBDC4', nomeEmpresa: 'Bradesco' }
      },
      {
        ...operacoes[0],
        id: 3,
        dataOperacao: '2026-09-08T09:00:00',
        valorTotal: 15,
        acao: { ...operacoes[0].acao, id: 3, ticker: 'ABEV3', nomeEmpresa: 'Ambev' }
      }
    ];
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const recentOperations = Array.from(host.querySelectorAll('.recent-operation'));
    const tickers = recentOperations.map(row =>
      row.querySelector('.recent-operation__asset strong')?.textContent?.trim()
    );

    expect(recentOperations.length).toBe(5);
    expect(tickers).toEqual(['MSFT', 'ITUB4', 'BBDC4', 'PETR4', 'VALE3']);
    expect(recentOperations[0].querySelector('.recent-operation__value')?.textContent)
      .toMatch(/US\$\s*1\.234,56/);
    expect(host.querySelector('[data-testid="valor-atual"]')?.textContent).toMatch(/R\$\s*420,00/);
    expect(host.querySelector('[data-testid="custo-total"]')?.textContent).toMatch(/R\$\s*360,00/);
    expect(host.querySelector('[data-testid="resultado"]')?.textContent).toMatch(/R\$\s*60,00/);
  });

  it('usa um contêiner neutro e preserva o main único do shell', () => {
    const host = fixture.nativeElement as HTMLElement;
    expect(host.firstElementChild?.matches('.page-shell.home-page')).toBeTrue();
    expect(host.firstElementChild?.tagName).toBe('DIV');
    expect(host.querySelector('main')).toBeNull();
  });

  it('oferece acesso direto à carteira e ao extrato sem duplicar cadastros', () => {
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('h1')?.textContent).toContain('Visão consolidada');
    expect(host.querySelector('a[href="/carteira"]')).not.toBeNull();
    expect(host.querySelector('a[href="/operacoes"]')).not.toBeNull();
    expect(host.querySelector('.quick-grid')).toBeNull();
    expect(host.querySelector('.domain-grid')).toBeNull();
  });
});
