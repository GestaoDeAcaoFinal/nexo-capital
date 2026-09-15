import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ApiError } from '../../core/api/api-error.interceptor';
import { Corretora } from '../../core/api/api.models';
import { CorretoraService } from '../../services/corretora.service';
import { CorretoraFormComponent } from './corretora-form.component';

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

function relativeLuminance(color: string): number {
  const channels = color.match(/[\d.]+/g)?.slice(0, 3).map(Number) ?? [];
  if (channels.length !== 3) throw new Error(`Cor CSS inválida: ${color}`);
  const [red, green, blue] = channels.map(channel => {
    const value = channel / 255;
    return value <= .03928 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
  });
  return .2126 * red + .7152 * green + .0722 * blue;
}

function contrastRatio(first: string, second: string): number {
  const lighter = Math.max(relativeLuminance(first), relativeLuminance(second));
  const darker = Math.min(relativeLuminance(first), relativeLuminance(second));
  return (lighter + .05) / (darker + .05);
}

describe('CorretoraFormComponent', () => {
  const corretora: Corretora = {
    id: 1, cnpj: '12345678000190', razaoSocial: 'Corretora Teste SA',
    nomeFantasia: 'Corretora Teste', email: null, telefone: null, cep: '01001000',
    logradouro: null, numero: null, complemento: null, bairro: null, cidade: 'São Paulo',
    uf: 'SP', situacaoCadastral: 'ATIVA', validadaNaCvm: true,
    dataCadastro: '2026-09-04T08:00:00'
  };
  let component: CorretoraFormComponent;
  let fixture: ComponentFixture<CorretoraFormComponent>;
  let service: jasmine.SpyObj<CorretoraService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<CorretoraService>('CorretoraService', ['salvar']);
    await TestBed.configureTestingModule({
      imports: [CorretoraFormComponent],
      providers: [
        provideRouter([]),
        { provide: CorretoraService, useValue: service }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CorretoraFormComponent);
    component = fixture.componentInstance;
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
      .toEqual(['/']);
    expect(breadcrumb?.querySelector('[aria-current="page"]')?.textContent).toContain('Nova corretora');
    expect(host.querySelector('form.finance-form')).not.toBeNull();
    expect(host.querySelector('.form-actions')).not.toBeNull();
    expect(host.querySelector('.form-actions a')?.getAttribute('href')).toBe('/corretoras');
    expectAccessibleFields(host, ['cnpj', 'cep']);
  });

  it('mantém a identificação e os vínculos acessíveis do cadastro', () => {
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.form-panel .form-section-title')?.textContent)
      .toContain('Identificação da instituição');
    expect(host.querySelector('#cnpj')?.getAttribute('aria-describedby')).toContain('cnpj-error');
    expect(host.querySelector('#cep')?.getAttribute('aria-describedby')).toContain('cep-error');
    expect(host.querySelector('.form-actions a')?.getAttribute('href')).toBe('/corretoras');
  });

  it('mantém contraste AA no breadcrumb', () => {
    fixture.detectChanges();
    const breadcrumb = fixture.nativeElement.querySelector('.breadcrumb-nav') as HTMLElement;
    const foreground = getComputedStyle(breadcrumb).color;
    const background = getComputedStyle(document.body).backgroundColor;

    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(4.5);
  });

  it('não envia CNPJ e CEP inválidos', () => {
    component.form.setValue({ cnpj: '123', cep: '12A' });

    component.salvar();

    expect(component.form.invalid).toBeTrue();
    expect(service.salvar).not.toHaveBeenCalled();
  });

  it('bloqueia duplicidade enquanto salva e apresenta a resposta enriquecida', () => {
    const response = new Subject<Corretora>();
    service.salvar.and.returnValue(response);
    component.form.setValue({ cnpj: '12.345.678/0001-90', cep: '01001-000' });

    component.salvar();
    component.salvar();

    expect(service.salvar).toHaveBeenCalledOnceWith({
      cnpj: '12.345.678/0001-90',
      cep: '01001-000'
    });
    expect(component.state.loading).toBeTrue();

    response.next(corretora);
    response.complete();

    expect(component.state.loading).toBeFalse();
    expect(component.state.data).toEqual(corretora);
    expect(component.state.successMessage).toBe('Corretora cadastrada com sucesso.');
  });

  it('preserva valores e associa erros do backend aos campos', () => {
    service.salvar.and.returnValue(throwError(() => new ApiError(
      409,
      'Conflito',
      'CNPJ já cadastrado',
      { cnpj: ['CNPJ já cadastrado'] }
    )));
    component.form.setValue({ cnpj: '12345678000190', cep: '01001000' });

    component.salvar();

    expect(component.form.getRawValue()).toEqual({
      cnpj: '12345678000190',
      cep: '01001000'
    });
    expect(component.form.controls.cnpj.errors?.['server']).toEqual(['CNPJ já cadastrado']);
    expect(component.state.error?.message).toBe('CNPJ já cadastrado');
  });

  it('oculta o cadastro anterior durante uma nova submissão que termina em erro', () => {
    const secondRequest = new Subject<Corretora>();
    service.salvar.and.returnValues(of(corretora), secondRequest);
    component.form.setValue({ cnpj: '12345678000190', cep: '01001000' });

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).not.toBeNull();

    component.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();

    secondRequest.error(new ApiError(409, 'Conflito', 'CNPJ já cadastrado'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('CNPJ já cadastrado');
    expect(fixture.nativeElement.querySelector('.result-card')).toBeNull();
  });
});
