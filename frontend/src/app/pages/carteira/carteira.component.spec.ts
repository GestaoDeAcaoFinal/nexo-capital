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

  it('formats every monetary metric and the four monetary columns of an international position in BRL', () => {
    const host = fixture.nativeElement as HTMLElement;
    const text = (element: Element | null) => element?.textContent?.replace(/\s+/g, ' ').trim();
    const monetaryMetrics = [
      ['valor-atual', 'R$ 1.100,00'],
      ['custo-total', 'R$ 1.050,00'],
      ['resultado', 'R$ 50,00']
    ];

    for (const [testId, expected] of monetaryMetrics) {
      expect(text(host.querySelector(`[data-testid="${testId}"]`))).toBe(expected);
    }

    const cells = Array.from(host.querySelectorAll('[data-testid="posicao-AAPL"] td.numeric'));
    expect(cells).toHaveSize(5);
    expect(cells.slice(1).map(cell => text(cell))).toEqual([
      'R$ 525,00',
      'R$ 1.050,00',
      'R$ 550,00',
      'R$ 1.100,00'
    ]);
  });

  it('keeps long negative monetary values visible across desktop and tablet metric grids', () => {
    const host = fixture.nativeElement as HTMLElement;
    const mediaRules = Array.from(document.styleSheets)
      .flatMap(sheet => Array.from(sheet.cssRules))
      .filter((rule): rule is CSSMediaRule => rule instanceof CSSMediaRule);
    const tabletRule = mediaRules.find(rule => rule.conditionText === '(max-width: 1199px)');
    const compactRule = mediaRules.find(rule => rule.conditionText === '(max-width: 820px)');

    expect(tabletRule).toBeDefined();
    expect(compactRule).toBeDefined();
    if (!tabletRule || !compactRule) return;

    const tabletMedia = tabletRule.media.mediaText;
    const compactMedia = compactRule.media.mediaText;
    const metrics = host.querySelector<HTMLElement>('.portfolio-metrics');
    const result = host.querySelector<HTMLElement>('[data-testid="resultado"]');
    expect(metrics).not.toBeNull();
    expect(result).not.toBeNull();

    const metricWidth = metrics!.style.width;
    const resultText = result!.textContent;
    const resultFontSize = result!.style.fontSize;

    try {
      tabletRule.media.mediaText = 'not all';
      compactRule.media.mediaText = 'not all';
      metrics!.style.width = '1180px';
      result!.textContent = '-R$ 9.999.999.999,99';
      result!.style.fontSize = '25.6px';
      expect(getComputedStyle(metrics!).gridTemplateColumns.trim().split(' ')).toHaveSize(3);
      expect(result!.scrollWidth).toBeLessThanOrEqual(result!.clientWidth);

      tabletRule.media.mediaText = 'all';
      metrics!.style.width = '868px';
      result!.style.fontSize = '18px';
      expect(getComputedStyle(metrics!).gridTemplateColumns.trim().split(' ')).toHaveSize(3);
      expect(result!.scrollWidth).toBeLessThanOrEqual(result!.clientWidth);

      compactRule.media.mediaText = 'all';
      metrics!.style.width = '788px';
      expect(getComputedStyle(metrics!).gridTemplateColumns.trim().split(' ')).toHaveSize(2);
      expect(result!.scrollWidth).toBeLessThanOrEqual(result!.clientWidth);
    } finally {
      tabletRule.media.mediaText = tabletMedia;
      compactRule.media.mediaText = compactMedia;
      metrics!.style.width = metricWidth;
      result!.textContent = resultText;
      result!.style.fontSize = resultFontSize;
    }
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

  it('mantém a seção da carteira nomeada por um título existente no estado vazio', () => {
    service.resumirCarteira.and.returnValue(of({ ...resumo, posicoes: [], quantidadeTotal: 0 }));
    component.carregarCarteira();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const section = host.querySelector<HTMLElement>('section[aria-labelledby]');
    const labelId = section?.getAttribute('aria-labelledby');
    const label = labelId ? host.querySelector(`#${labelId}`) : null;

    expect(section).not.toBeNull();
    expect(label).not.toBeNull();
    expect(label?.textContent?.trim()).toBe('Carteira consolidada');
  });
});
