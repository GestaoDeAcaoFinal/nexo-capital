import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ApiError } from '../../core/api/api-error.interceptor';
import { Corretora } from '../../core/api/api.models';
import { CorretoraService } from '../../services/corretora.service';
import { CorretoraListComponent } from './corretora-list.component';

describe('CorretoraListComponent', () => {
  const corretora: Corretora = {
    id: 1, cnpj: '12345678000190', razaoSocial: 'Corretora Teste SA',
    nomeFantasia: 'Corretora Teste', email: null, telefone: null, cep: '01001000',
    logradouro: null, numero: null, complemento: null, bairro: null, cidade: 'São Paulo',
    uf: 'SP', situacaoCadastral: 'ATIVA', validadaNaCvm: true,
    dataCadastro: '2026-09-04T08:00:00'
  };
  let component: CorretoraListComponent;
  let fixture: ComponentFixture<CorretoraListComponent>;
  let service: jasmine.SpyObj<CorretoraService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<CorretoraService>(
      'CorretoraService',
      ['listar', 'buscarPorId', 'buscarPorCnpj']
    );
    service.listar.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [CorretoraListComponent],
      providers: [
        provideRouter([]),
        { provide: CorretoraService, useValue: service }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CorretoraListComponent);
    component = fixture.componentInstance;
  });

  it('distingue carregamento de lista vazia', () => {
    const response = new Subject<Corretora[]>();
    service.listar.and.returnValue(response);

    fixture.detectChanges();
    expect(component.listState.loading).toBeTrue();
    expect(component.listState.data).toBeNull();

    response.next([]);
    response.complete();
    expect(component.listState.loading).toBeFalse();
    expect(component.listState.data).toEqual([]);
  });

  it('mantém erro de listagem e permite nova tentativa', () => {
    service.listar.and.returnValues(
      throwError(() => new ApiError(503, 'Indisponível', 'Tente novamente')),
      of([corretora])
    );

    fixture.detectChanges();
    expect(component.listState.error?.message).toBe('Tente novamente');

    component.carregarCorretoras();
    expect(component.listState.data).toEqual([corretora]);
    expect(service.listar).toHaveBeenCalledTimes(2);
  });

  it('não busca com id não positivo', () => {
    fixture.detectChanges();
    component.searchForm.setValue({ criterio: 'id', valor: '0' });

    component.buscar();

    expect(service.buscarPorId).not.toHaveBeenCalled();
    expect(component.searchForm.controls.valor.invalid).toBeTrue();
  });

  it('busca por CNPJ válido e apresenta detalhes', () => {
    service.buscarPorCnpj.and.returnValue(of(corretora));
    fixture.detectChanges();
    component.searchForm.setValue({ criterio: 'cnpj', valor: '12.345.678/0001-90' });

    component.buscar();

    expect(service.buscarPorCnpj).toHaveBeenCalledOnceWith('12.345.678/0001-90');
    expect(component.searchState.data).toEqual(corretora);
  });

  it('oculta o resultado anterior quando uma nova consulta falha', () => {
    service.buscarPorId.and.returnValues(
      of(corretora),
      throwError(() => new ApiError(404, 'Não encontrada', 'Corretora não encontrada.'))
    );
    fixture.detectChanges();

    component.searchForm.setValue({ criterio: 'id', valor: '1' });
    component.buscar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.search-result')).not.toBeNull();

    component.searchForm.setValue({ criterio: 'id', valor: '2' });
    component.buscar();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Corretora não encontrada.');
    expect(fixture.nativeElement.querySelector('.search-result')).toBeNull();
  });

  it('apresenta fallbacks para razão social e localidade ausentes', () => {
    fixture.detectChanges();
    component.listState.data = [{
      ...corretora,
      razaoSocial: null as unknown as string,
      cidade: null,
      uf: null
    }];
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Razão social não informada');
    expect(fixture.nativeElement.textContent).toContain('Localidade não informada');
  });

  it('destaca o estado cadastral sem alterar os dados da consulta', () => {
    fixture.detectChanges();
    component.listState.data = [corretora];
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.query-panel--compact')).not.toBeNull();
    expect(host.querySelector('.entity-status--verified')?.textContent).toContain('Validada na CVM');
    expect(host.querySelector('[data-testid="corretora-count"]')?.textContent).toContain('1 registro');
  });

  it('organiza consulta e resultados como uma listagem financeira', () => {
    service.listar.and.returnValue(of([corretora]));
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('div.page-shell')).not.toBeNull();
    expect(host.querySelector('section.filter-panel')).not.toBeNull();
    expect(host.querySelector('a.btn-finance-primary')).not.toBeNull();
    const table = host.querySelector('table.finance-table') as HTMLTableElement;
    expect(table.classList).toContain('table-hover');
    expect(getComputedStyle(table).getPropertyValue('--bs-table-hover-bg').trim()).toBe('#f4f8fa');
  });
});
