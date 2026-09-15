import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, Corretora } from '../../core/api/api.models';
import { AcaoService } from '../../services/acao.service';
import { AcaoListComponent } from './acao-list.component';

const corretora: Corretora = { id: 2, cnpj: '12345678000190', razaoSocial: 'XP', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: null, numero: null, complemento: null, bairro: null, cidade: 'São Paulo', uf: 'SP', situacaoCadastral: null, validadaNaCvm: true, dataCadastro: null };
const acao: Acao = { id: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'B3', moeda: 'BRL', cotacaoAtual: 35, dataHoraCotacao: null, corretoraRelacionada: corretora };

registerLocaleData(localePt, 'pt-BR');

function relativeLuminance(color: string): number {
  const channels = color.match(/[\d.]+/g)?.slice(0, 3).map(Number) ?? [];
  if (channels.length !== 3) throw new Error(`Cor CSS inválida: ${color}`);
  const [red, green, blue] = channels.map(channel => {
    const value = channel / 255;
    return value <= .03928 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
  });
  return .2126 * red + .7152 * green + .0722 * blue;
}

function normalizeCssColor(color: string): string {
  const probe = document.createElement('span');
  probe.style.color = color;
  document.body.appendChild(probe);
  const normalized = getComputedStyle(probe).color;
  probe.remove();
  return normalized;
}

function contrastRatio(first: string, second: string): number {
  const lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
  const darker = Math.min(relativeLuminance(first), relativeLuminance(second));
  return (lighter + .05) / (darker + .05);
}

describe('AcaoListComponent', () => {
  let fixture: ComponentFixture<AcaoListComponent>; let component: AcaoListComponent; let service: jasmine.SpyObj<AcaoService>;
  beforeEach(async () => {
    service = jasmine.createSpyObj('AcaoService', ['listar', 'buscarPorId', 'buscarPorTicker', 'atualizarCotacao']); service.listar.and.returnValue(of([]));
    await TestBed.configureTestingModule({ imports: [AcaoListComponent], providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'pt-BR' }, { provide: AcaoService, useValue: service }] }).compileComponents();
    fixture = TestBed.createComponent(AcaoListComponent); component = fixture.componentInstance; fixture.detectChanges();
  });
  it('representa lista vazia e permite retry após erro', () => {
    expect(component.listState.data).toEqual([]);
    service.listar.and.returnValue(throwError(() => new ApiError(503, 'Indisponível', 'Tente depois.'))); component.carregarAcoes();
    expect(component.listState.error?.status).toBe(503);
    service.listar.and.returnValue(of([acao])); component.carregarAcoes(); expect(component.listState.data).toEqual([acao]);
  });
  it('bloqueia consulta inválida e busca ticker válido', () => {
    component.searchForm.setValue({ criterio: 'id', valor: '0' }); component.buscar(); expect(service.buscarPorId).not.toHaveBeenCalled();
    service.buscarPorTicker.and.returnValue(of(acao)); component.searchForm.setValue({ criterio: 'ticker', valor: ' petr4 ' }); component.buscar();
    expect(service.buscarPorTicker).toHaveBeenCalledOnceWith('PETR4'); expect(component.searchState.data).toEqual(acao);
  });
  it('atualiza a ação visível e preserva dados se provedor falhar', () => {
    component.listState.data = [acao]; const atualizada = { ...acao, cotacaoAtual: 36 };
    service.atualizarCotacao.and.returnValue(of(atualizada)); component.atualizarCotacao(acao); expect(component.listState.data?.[0].cotacaoAtual).toBe(36);
    service.atualizarCotacao.and.returnValue(throwError(() => new ApiError(502, 'Provedor', 'Falhou.'))); component.atualizarCotacao(atualizada);
    expect(component.listState.data?.[0].cotacaoAtual).toBe(36); expect(component.updateErrors[1].status).toBe(502);
  });

  it('oculta o resultado anterior quando uma nova consulta falha', () => {
    service.buscarPorId.and.returnValues(
      of(acao),
      throwError(() => new ApiError(404, 'Não encontrada', 'Ação não encontrada.'))
    );

    component.searchForm.setValue({ criterio: 'id', valor: '1' });
    component.buscar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.search-result')).not.toBeNull();

    component.searchForm.setValue({ criterio: 'id', valor: '2' });
    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ação não encontrada.');
    expect(fixture.nativeElement.querySelector('.search-result')).toBeNull();
  });

  it('organiza consulta e resultados como uma listagem financeira', () => {
    component.listState.data = [acao];
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('div.page-shell')).not.toBeNull();
    expect(host.querySelector('section.filter-panel')).not.toBeNull();
    expect(host.querySelector('a.btn-finance-primary')).not.toBeNull();
    const table = host.querySelector('table.finance-table') as HTMLTableElement;
    expect(table.classList).toContain('table-hover');
    expect(getComputedStyle(table).getPropertyValue('--bs-table-hover-bg').trim()).toBe('#f4f8fa');
    expect(table.querySelector('thead th')?.textContent?.trim()).toBe('ID');
    const idCell = table.querySelector('tbody td') as HTMLTableCellElement;
    expect(idCell.classList).toContain('numeric');
    expect(idCell.textContent?.trim()).toBe('1');
  });

  it('separa mercado e moeda da cotação internacional', () => {
    component.listState.data = [{ ...acao, mercado: 'NASDAQ', moeda: 'USD', cotacaoAtual: 190 }];
    fixture.detectChanges();

    const row = fixture.nativeElement.querySelector('tbody tr') as HTMLTableRowElement;
    expect(row.querySelector('.market-badge')?.textContent).toContain('NASDAQ');
    expect(row.querySelector('.currency-code')?.textContent).toContain('USD');
    expect(row.textContent).toContain('US$');
  });

  it('mantém contraste AA do texto secundário sobre o hover da tabela', () => {
    component.listState.data = [acao];
    fixture.detectChanges();
    const table = fixture.nativeElement.querySelector('table.finance-table') as HTMLTableElement;
    const secondary = table.querySelector('.cell-secondary') as HTMLElement;
    const hoverBackground = normalizeCssColor(
      getComputedStyle(table).getPropertyValue('--bs-table-hover-bg').trim()
    );

    expect(contrastRatio(getComputedStyle(secondary).color, hoverBackground))
      .toBeGreaterThanOrEqual(4.5);
  });
});
