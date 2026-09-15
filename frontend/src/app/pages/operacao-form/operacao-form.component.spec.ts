import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, Corretora, OperacaoAcao } from '../../core/api/api.models';
import { AcaoService } from '../../services/acao.service';
import { OperacaoService } from '../../services/operacao.service';
import { OperacaoFormComponent } from './operacao-form.component';

const corretora: Corretora = { id: 1, cnpj: '12345678000190', razaoSocial: 'XP', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: null, numero: null, complemento: null, bairro: null, cidade: null, uf: null, situacaoCadastral: null, validadaNaCvm: true, dataCadastro: null };
const acao: Acao = { id: 3, ticker: 'PETR4', nomeEmpresa: null, mercado: 'B3', moeda: 'BRL', cotacaoAtual: 35, dataHoraCotacao: null, corretoraRelacionada: corretora };
const operacao: OperacaoAcao = { id: 4, quantidade: 2, precoUnitario: 35, precoMedio: 35, lucroPrejuizo: null, dataOperacao: '2026-09-04T10:00:00', valorTotal: 70, tipoOperacao: 'COMPRA', acao };

registerLocaleData(localePt, 'pt-BR');

function expectAccessibleFields(host: HTMLElement, fieldIds: string[]): void {
  const ids = Array.from(host.querySelectorAll<HTMLElement>('[id]'), element => element.id);
  expect(new Set(ids).size).withContext('IDs únicos').toBe(ids.length);

  for (const fieldId of fieldIds) {
    expect(host.querySelector(`#${fieldId}`)).withContext(`controle #${fieldId}`).not.toBeNull();
    expect(host.querySelector(`label[for="${fieldId}"]`))
      .withContext(`label de #${fieldId}`)
      .not.toBeNull();
  }

  for (const control of host.querySelectorAll<HTMLElement>('[aria-describedby]')) {
    const references = control.getAttribute('aria-describedby')?.split(/\s+/) ?? [];
    expect(references.length).withContext(`descrições de #${control.id}`).toBeGreaterThan(0);
    for (const reference of references) {
      expect(host.querySelector(`#${reference}`))
        .withContext(`#${control.id} descreve #${reference}`)
        .not.toBeNull();
    }
  }
}

describe('OperacaoFormComponent', () => {
  let fixture: ComponentFixture<OperacaoFormComponent>; let component: OperacaoFormComponent; let service: jasmine.SpyObj<OperacaoService>; let acaoService: jasmine.SpyObj<AcaoService>;
  beforeEach(async () => {
    service = jasmine.createSpyObj('OperacaoService', ['comprar', 'vender']); acaoService = jasmine.createSpyObj('AcaoService', ['listar']); acaoService.listar.and.returnValue(of([acao]));
    await TestBed.configureTestingModule({ imports: [OperacaoFormComponent], providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'pt-BR' }, { provide: OperacaoService, useValue: service }, { provide: AcaoService, useValue: acaoService }] }).compileComponents();
    fixture = TestBed.createComponent(OperacaoFormComponent); component = fixture.componentInstance; fixture.detectChanges();
  });
  it('renderiza o fluxo em um painel financeiro acessível', () => {
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const page = host.querySelector('.page-shell');
    const breadcrumb = host.querySelector('nav[aria-label="Navegação estrutural"]');

    expect(page?.tagName).toBe('DIV');
    expect(host.querySelector('main')).toBeNull();
    expect(breadcrumb).not.toBeNull();
    expect(Array.from(breadcrumb?.querySelectorAll('a') ?? [], link => link.getAttribute('href')))
      .toEqual(['/', '/operacoes']);
    expect(breadcrumb?.querySelector('[aria-current="page"]')?.textContent).toContain('Nova operação');
    expect(host.querySelector('form.finance-form')).not.toBeNull();
    expect(host.querySelector('.form-actions')).not.toBeNull();
    expect(host.querySelector('.form-actions a')?.getAttribute('href')).toBe('/operacoes');

    expectAccessibleFields(host, ['acao', 'quantidade', 'tipo-compra', 'tipo-venda', 'preco-compra']);
    component.form.controls.tipoOperacao.setValue('VENDA');
    fixture.detectChanges();
    expectAccessibleFields(host, ['acao', 'quantidade', 'tipo-compra', 'tipo-venda', 'preco']);
  });
  it('carrega ações e rejeita quantidade não inteira positiva', () => {
    expect(component.acoesState.data).toEqual([acao]); component.form.setValue({ acaoId: 3, quantidade: 1.5, tipoOperacao: 'COMPRA', precoCompra: 35, precoVenda: null }); component.salvar(); expect(service.comprar).not.toHaveBeenCalled();
  });
  it('apresenta os estados do formulário em painéis compartilhados sem alertas Bootstrap', () => {
    component.acoesState = { loading: true, data: null, successMessage: null, error: null };
    fixture.detectChanges();

    let host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.status-panel[role="status"][aria-live="polite"]')?.textContent)
      .toContain('Carregando ações');
    expect(host.querySelectorAll('.alert').length).toBe(0);

    component.acoesState = { loading: false, data: null, successMessage: null, error: new ApiError(503, 'Indisponível', 'Ações indisponíveis.') };
    fixture.detectChanges();
    host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.status-panel.status-panel--error[role="alert"]')?.textContent)
      .toContain('Ações indisponíveis.');
    expect(host.querySelector('.status-panel button')?.textContent).toContain('Tentar novamente');

    component.acoesState = { loading: false, data: [acao], successMessage: null, error: null };
    component.state = { loading: false, data: null, successMessage: null, error: new ApiError(409, 'Conflito', 'Saldo insuficiente.') };
    fixture.detectChanges();
    host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.status-panel.status-panel--error[role="alert"]')?.textContent)
      .toContain('Conflito: Saldo insuficiente.');

    component.state = { loading: false, data: operacao, successMessage: 'Compra realizada com sucesso.', error: null };
    fixture.detectChanges();
    host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.status-panel[role="status"][aria-live="polite"]')?.textContent)
      .toContain('Compra realizada com sucesso.');
    expect(host.querySelectorAll('.alert').length).toBe(0);
  });
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
    expect(context.querySelector('strong')?.textContent).toContain('US$');
    expect(context.querySelector('strong')?.textContent).toContain('190,00');
    expect(fixture.nativeElement.querySelector('#preco-compra-help')?.textContent)
      .toContain('Valor informado em USD');
  });
  it('informa os fallbacks quando o ativo selecionado não possui mercado, moeda ou cotação', () => {
    const semDados = { ...acao, id: 10, ticker: 'MISSING', mercado: null, moeda: null, cotacaoAtual: null };
    component.acoesState.data = [semDados];
    component.form.controls.acaoId.setValue(10);
    fixture.detectChanges();

    const context = fixture.nativeElement.querySelector('.asset-context') as HTMLElement;
    expect(component.moedaSelecionada).toBe('BRL');
    expect(context.textContent).toContain('Mercado não informado');
    expect(context.textContent).toContain('BRL');
    expect(context.querySelector('strong')?.textContent).toContain('Cotação indisponível');
    expect(fixture.nativeElement.querySelector('#preco-compra-help')?.textContent)
      .toContain('Valor informado em BRL');
  });
  it('compra com preço preenchido e bloqueia submissão duplicada', () => {
    const pending = new Subject<OperacaoAcao>(); service.comprar.and.returnValue(pending); component.form.setValue({ acaoId: 3, quantidade: 2, tipoOperacao: 'COMPRA', precoCompra: 35, precoVenda: null }); component.salvar(); component.salvar();
    expect(service.comprar).toHaveBeenCalledOnceWith(3, 2, 35); pending.next(operacao); expect(component.state.data).toEqual(operacao);
  });
  it('preenche o preço de compra com a cotação e permite editar antes de salvar', () => {
    component.form.controls.acaoId.setValue(3);
    const precoCompra = component.form.get('precoCompra');

    expect(precoCompra).not.toBeNull();
    expect(precoCompra?.value).toBe(35);

    precoCompra?.setValue(30);
    component.form.controls.quantidade.setValue(1);
    service.comprar.and.returnValue(of({ ...operacao, quantidade: 1, precoUnitario: 30, precoMedio: 30, valorTotal: 30 }));
    component.salvar();

    expect(service.comprar).toHaveBeenCalledOnceWith(3, 1, 30);
  });
  it('exige preço positivo para venda e preserva valores no conflito', () => {
    component.form.setValue({ acaoId: 3, quantidade: 2, tipoOperacao: 'VENDA', precoCompra: null, precoVenda: null }); component.salvar(); expect(service.vender).not.toHaveBeenCalled();
    service.vender.and.returnValue(throwError(() => new ApiError(409, 'Conflito', 'Saldo insuficiente.'))); component.form.controls.precoVenda.setValue(40); component.salvar();
    expect(service.vender).toHaveBeenCalledOnceWith(3, 2, 40); expect(component.form.getRawValue().precoVenda).toBe(40); expect(component.state.error?.status).toBe(409);
  });
  it('formata total e data da operação no locale pt-BR', () => {
    service.comprar.and.returnValue(of(operacao));
    component.form.setValue({ acaoId: 3, quantidade: 2, tipoOperacao: 'COMPRA', precoCompra: 35, precoVenda: null });

    component.salvar();
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('.result-card') as HTMLElement;
    const content = result.textContent?.replace(/\s/g, ' ');
    expect(content).toContain('R$ 70,00');
    expect(content).toContain('04/09/2026 às 10:00');
  });

  it('oculta a operação anterior durante uma nova submissão que termina em erro', () => {
    const secondRequest = new Subject<OperacaoAcao>();
    service.comprar.and.returnValues(of(operacao), secondRequest);
    component.form.setValue({ acaoId: 3, quantidade: 2, tipoOperacao: 'COMPRA', precoCompra: 35, precoVenda: null });

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).not.toBeNull();

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();

    secondRequest.error(new ApiError(409, 'Conflito', 'Saldo insuficiente'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Saldo insuficiente');
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();
  });
});
