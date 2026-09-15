import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, Corretora } from '../../core/api/api.models';
import { AcaoService } from '../../services/acao.service';
import { CorretoraService } from '../../services/corretora.service';
import { AcaoFormComponent } from './acao-form.component';

const corretora: Corretora = { id: 2, cnpj: '12345678000190', razaoSocial: 'XP', nomeFantasia: null, email: null, telefone: null, cep: '01001000', logradouro: null, numero: null, complemento: null, bairro: null, cidade: 'São Paulo', uf: 'SP', situacaoCadastral: null, validadaNaCvm: true, dataCadastro: null };
const acao: Acao = { id: 1, ticker: 'PETR4', nomeEmpresa: 'Petrobras', mercado: 'B3', moeda: 'BRL', cotacaoAtual: 35, dataHoraCotacao: '2026-09-04T10:00:00', corretoraRelacionada: corretora };

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

describe('AcaoFormComponent', () => {
  let fixture: ComponentFixture<AcaoFormComponent>;
  let component: AcaoFormComponent;
  let acaoService: jasmine.SpyObj<AcaoService>;
  let corretoraService: jasmine.SpyObj<CorretoraService>;
  beforeEach(async () => {
    acaoService = jasmine.createSpyObj('AcaoService', ['salvar']);
    corretoraService = jasmine.createSpyObj('CorretoraService', ['listar']);
    corretoraService.listar.and.returnValue(of([corretora]));
    await TestBed.configureTestingModule({ imports: [AcaoFormComponent], providers: [provideRouter([]), { provide: LOCALE_ID, useValue: 'pt-BR' }, { provide: AcaoService, useValue: acaoService }, { provide: CorretoraService, useValue: corretoraService }] }).compileComponents();
    fixture = TestBed.createComponent(AcaoFormComponent); component = fixture.componentInstance; fixture.detectChanges();
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
      .toEqual(['/', '/acoes']);
    expect(breadcrumb?.querySelector('[aria-current="page"]')?.textContent).toContain('Nova ação');
    expect(host.querySelector('form.finance-form')).not.toBeNull();
    expect(host.querySelector('.form-section-title')?.textContent).toContain('Identificação do ativo');
    expect(host.querySelector('.form-actions')).not.toBeNull();
    expect(host.querySelector('.form-actions a')?.getAttribute('href')).toBe('/acoes');
    expectAccessibleFields(host, ['ticker', 'corretora']);
  });

  it('apresenta carregamento e erro de corretoras em painéis acessíveis', () => {
    component.corretorasState = { loading: true, data: null, successMessage: null, error: null };
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.status-panel[role="status"]')?.textContent).toContain('Carregando corretoras');

    component.corretorasState = {
      loading: false,
      data: null,
      successMessage: null,
      error: new ApiError(503, 'Indisponível', 'Tente novamente.')
    };
    fixture.detectChanges();

    const errorPanel = host.querySelector('.status-panel--error[role="alert"]') as HTMLElement;
    expect(errorPanel.textContent).toContain('Tente novamente.');
    expect(errorPanel.querySelector('button.btn-finance-secondary')?.textContent).toContain('Tentar novamente');
  });
  it('carrega corretoras e não envia formulário inválido', () => {
    expect(component.corretorasState.data).toEqual([corretora]);
    component.form.setValue({ ticker: 'P', corretoraId: 0 }); component.salvar();
    expect(acaoService.salvar).not.toHaveBeenCalled();
  });
  it('envia somente ticker e corretoraId e bloqueia duplicidade', () => {
    const pending = new Subject<Acao>(); acaoService.salvar.and.returnValue(pending);
    component.form.setValue({ ticker: 'PETR4', corretoraId: 2 }); component.salvar(); component.salvar();
    expect(acaoService.salvar).toHaveBeenCalledOnceWith({ ticker: 'PETR4', corretoraId: 2 });
    pending.next(acao); expect(component.state.data).toEqual(acao);
  });
  it('preserva valores e exibe somente os erros específicos devolvidos pelo backend', () => {
    acaoService.salvar.and.returnValue(throwError(() => new ApiError(
      422,
      'Dados inválidos',
      'Revise os campos.',
      { ticker: ['Ticker não negociado'], corretoraId: ['Corretora bloqueada'] }
    )));
    component.form.setValue({ ticker: 'PETR4', corretoraId: 2 });
    component.form.markAllAsTouched();
    component.salvar();
    fixture.detectChanges();

    expect(component.form.getRawValue()).toEqual({ ticker: 'PETR4', corretoraId: 2 });
    expect(component.form.controls.ticker.errors?.['server']).toEqual(['Ticker não negociado']);
    expect(component.form.controls.corretoraId.errors?.['server']).toEqual(['Corretora bloqueada']);
    expect(fixture.nativeElement.querySelector('#ticker-error')?.textContent?.trim())
      .toBe('Ticker não negociado');
    expect(fixture.nativeElement.querySelector('#corretora-error')?.textContent?.trim())
      .toBe('Corretora bloqueada');
  });
  it('formata a cotação cadastrada em real no locale pt-BR', () => {
    acaoService.salvar.and.returnValue(of(acao));
    component.form.setValue({ ticker: 'PETR4', corretoraId: 2 });

    component.salvar();
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('.result-card') as HTMLElement;
    expect(result.textContent?.replace(/\s/g, ' ')).toContain('R$ 35,00');
  });

  it('oculta o cadastro anterior durante uma nova submissão que termina em erro', () => {
    const secondRequest = new Subject<Acao>();
    acaoService.salvar.and.returnValues(of(acao), secondRequest);
    component.form.setValue({ ticker: 'PETR4', corretoraId: 2 });

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).not.toBeNull();

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();

    secondRequest.error(new ApiError(503, 'Indisponível', 'Cotação indisponível'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Cotação indisponível');
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();
  });
});
