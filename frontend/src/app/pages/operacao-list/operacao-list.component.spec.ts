import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, Corretora, OperacaoAcao } from '../../core/api/api.models';
import { AcaoService } from '../../services/acao.service';
import { OperacaoService } from '../../services/operacao.service';
import { OperacaoListComponent } from './operacao-list.component';

const corretora: Corretora = { id: 1, cnpj: '12345678000190', razaoSocial: 'XP', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: null, numero: null, complemento: null, bairro: null, cidade: null, uf: null, situacaoCadastral: null, validadaNaCvm: true, dataCadastro: null };
const acao: Acao = { id: 3, ticker: 'PETR4', nomeEmpresa: null, mercado: 'B3', moeda: 'BRL', cotacaoAtual: 35, dataHoraCotacao: null, corretoraRelacionada: corretora };
const op: OperacaoAcao = { id: 4, quantidade: 2, precoUnitario: 35, precoMedio: null, lucroPrejuizo: null, dataOperacao: '2026-09-04T10:00:00', valorTotal: 70, tipoOperacao: 'COMPRA', acao };
describe('OperacaoListComponent', () => {
  let fixture: ComponentFixture<OperacaoListComponent>; let component: OperacaoListComponent; let service: jasmine.SpyObj<OperacaoService>; let acaoService: jasmine.SpyObj<AcaoService>;
  beforeEach(async () => {
    service = jasmine.createSpyObj('OperacaoService', ['listar', 'listarCompras', 'listarVendas', 'listarHistorico', 'buscarPorId', 'resumirCarteira']); service.listar.and.returnValue(of([]));
    acaoService = jasmine.createSpyObj('AcaoService', ['listar']); acaoService.listar.and.returnValue(of([acao]));
    await TestBed.configureTestingModule({ imports: [OperacaoListComponent], providers: [provideRouter([]), { provide: OperacaoService, useValue: service }, { provide: AcaoService, useValue: acaoService }] }).compileComponents();
    fixture = TestBed.createComponent(OperacaoListComponent); component = fixture.componentInstance; fixture.detectChanges();
  });
  it('representa vazio e retry após erro', () => { expect(component.listState.data).toEqual([]); service.listar.and.returnValue(throwError(() => new ApiError(503, 'Erro', 'Falhou.'))); component.carregar(); expect(component.listState.error).not.toBeNull(); service.listar.and.returnValue(of([op])); component.carregar(); expect(component.listState.data).toEqual([op]); });
  it('usa exatamente o endpoint do filtro selecionado', () => { service.listarCompras.and.returnValue(of([op])); component.aplicarFiltro('compras'); expect(service.listarCompras).toHaveBeenCalledTimes(1); service.listarVendas.and.returnValue(of([])); component.aplicarFiltro('vendas'); expect(service.listarVendas).toHaveBeenCalledTimes(1); service.listar.and.returnValue(of([op])); component.aplicarFiltro('todas'); expect(service.listar).toHaveBeenCalledTimes(2); });
  it('mantém o filtro selecionado e oferece retry após erro', () => { service.listarVendas.and.returnValue(throwError(() => new ApiError(503, 'Indisponível', 'Tente novamente.'))); component.aplicarFiltro('vendas'); expect(component.filtroAtivo).toBe('vendas'); expect(component.listState.error?.status).toBe(503); });
  it('consulta histórico da ação e mantém resultado vazio', () => { service.listarHistorico.and.returnValue(of([])); component.historicoForm.setValue({ acaoId: 3 }); component.buscarHistorico(); expect(service.listarHistorico).toHaveBeenCalledOnceWith(3); expect(component.listState.data).toEqual([]); });
  it('mantém o histórico selecionado quando a ação não existe', () => { service.listarHistorico.and.returnValue(throwError(() => new ApiError(404, 'Não encontrada', 'Ação não encontrada.'))); component.historicoForm.setValue({ acaoId: 3 }); component.buscarHistorico(); expect(component.filtroAtivo).toBe('historico'); expect(component.listState.error?.status).toBe(404); });
  it('valida e busca operação por id', () => { component.idForm.setValue({ id: 0 }); component.buscarPorId(); expect(service.buscarPorId).not.toHaveBeenCalled(); service.buscarPorId.and.returnValue(of(op)); component.idForm.setValue({ id: 4 }); component.buscarPorId(); expect(component.detailState.data).toEqual(op); });
  it('apresenta 404 ao buscar uma operação inexistente', () => { service.buscarPorId.and.returnValue(throwError(() => new ApiError(404, 'Não encontrada', 'Operação não encontrada.'))); component.idForm.setValue({ id: 99 }); component.buscarPorId(); expect(component.detailState.error?.status).toBe(404); });
  it('oculta o detalhe anterior quando uma nova consulta falha', () => {
    service.buscarPorId.and.returnValues(
      of(op),
      throwError(() => new ApiError(404, 'Não encontrada', 'Operação não encontrada.'))
    );

    component.idForm.setValue({ id: 4 }); component.buscarPorId(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.search-result')).not.toBeNull();

    component.idForm.setValue({ id: 99 }); component.buscarPorId(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Operação não encontrada.');
    expect(fixture.nativeElement.querySelector('.search-result')).toBeNull();
  });
  it('ignora uma resposta antiga após a seleção de outro filtro', () => {
    const compras = new Subject<OperacaoAcao[]>();
    const vendas = new Subject<OperacaoAcao[]>();
    const venda = { ...op, id: 5, tipoOperacao: 'VENDA' as const };
    service.listarCompras.and.returnValue(compras);
    service.listarVendas.and.returnValue(vendas);

    component.aplicarFiltro('compras');
    component.aplicarFiltro('vendas');
    vendas.next([venda]);
    compras.next([op]);

    expect(component.filtroAtivo).toBe('vendas');
    expect(component.listState.data).toEqual([venda]);
  });
  it('permite tentar novamente o carregamento de ações do filtro histórico', () => {
    component.acoesState = {
      loading: false,
      data: null,
      error: new ApiError(503, 'Indisponível', 'Ações indisponíveis.'),
      successMessage: null
    };
    acaoService.listar.and.returnValue(of([acao]));
    fixture.detectChanges();
    const retry = fixture.nativeElement.querySelector('.acoes-retry') as HTMLButtonElement | null;
    expect(retry).not.toBeNull();

    retry?.click();
    expect(acaoService.listar).toHaveBeenCalledTimes(2);
    expect(component.acoesState.data).toEqual([acao]);
  });
  it('torna o loading observável e formata valores financeiros nulos com marcador', () => {
    const pending = new Subject<OperacaoAcao[]>(); service.listarCompras.and.returnValue(pending); component.aplicarFiltro('compras'); fixture.detectChanges();
    expect(component.listState.loading).toBeTrue(); expect(fixture.nativeElement.textContent).toContain('Carregando operações');
    pending.next([op]); pending.complete(); fixture.detectChanges(); const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Não informado'); expect(text).not.toContain('NaN'); expect(text).not.toContain('Invalid Date');
  });

  it('expõe filtros textuais e tabela financeira', () => {
    const positive = { ...op, lucroPrejuizo: 12 };
    const negative = { ...op, id: 5, lucroPrejuizo: -7 };
    component.listState.data = [positive, negative];
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('[aria-label="Filtros de operações"]')).not.toBeNull();
    expect(host.querySelector('[role="group"][aria-label="Filtros de operações"]')).not.toBeNull();
    const table = host.querySelector('[aria-labelledby="historico-operacoes"] table.finance-table') as HTMLTableElement;
    expect(table.classList).toContain('table-hover');
    expect(getComputedStyle(table).getPropertyValue('--bs-table-hover-bg').trim()).toBe('#f4f8fa');
    expect(table.querySelector('thead th')?.textContent?.trim()).toBe('ID');
    const firstId = table.querySelector('tbody td') as HTMLTableCellElement;
    expect(firstId.classList).toContain('numeric');
    expect(firstId.textContent?.trim()).toBe('4');
    expect(host.querySelector('.trade-badge')?.textContent).toContain('COMPRA');
    expect(getComputedStyle(host.querySelector('.value-positive') as HTMLElement).color)
      .toBe('rgb(4, 120, 87)');
    expect(getComputedStyle(host.querySelector('.value-negative') as HTMLElement).color)
      .toBe('rgb(180, 35, 24)');
  });

  it('mantém operações independente do resumo da carteira', () => {
    expect(service.resumirCarteira).not.toHaveBeenCalled();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.portfolio-panel')).toBeNull();
    expect(host.querySelector('[data-testid="quantidade-total"]')).toBeNull();
    expect(host.querySelector('[aria-label="Filtros de operações"]')).not.toBeNull();
  });
});
