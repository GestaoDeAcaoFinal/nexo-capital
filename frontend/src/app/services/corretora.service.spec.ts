import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../core/api/api-base-url.token';
import { ApiError, apiErrorInterceptor } from '../core/api/api-error.interceptor';
import { Corretora, CorretoraRequest, ProblemDetail } from '../core/api/api.models';
import { CorretoraService } from './corretora.service';

describe('CorretoraService', () => {
  const apiBaseUrl = 'https://api.example.test';
  const corretora: Corretora = {
    id: 2,
    cnpj: '12345678000190',
    razaoSocial: 'Corretora Teste SA',
    nomeFantasia: 'Corretora Teste',
    email: 'contato@example.test',
    telefone: '1130000000',
    cep: '01001000',
    logradouro: 'Praça da Sé',
    numero: null,
    complemento: null,
    bairro: 'Sé',
    cidade: 'São Paulo',
    uf: 'SP',
    situacaoCadastral: 'ATIVA',
    validadaNaCvm: true,
    dataCadastro: '2026-09-04T08:00:00'
  };

  let service: CorretoraService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: apiBaseUrl }
      ]
    });
    service = TestBed.inject(CorretoraService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('envia somente o contrato de cadastro para POST /corretoras', async () => {
    const request: CorretoraRequest = { cnpj: '12345678000190', cep: '01001000' };
    const resultPromise = firstValueFrom(service.salvar(request));

    const req = httpTesting.expectOne(`${apiBaseUrl}/corretoras`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(corretora);

    expect(await resultPromise).toEqual(corretora);
  });

  it('lista corretoras com GET /corretoras', async () => {
    const resultPromise = firstValueFrom(service.listar());

    const req = httpTesting.expectOne(`${apiBaseUrl}/corretoras`);
    expect(req.request.method).toBe('GET');
    req.flush([corretora]);

    expect(await resultPromise).toEqual([corretora]);
  });

  it('busca corretora por id com GET /corretoras/{id}', async () => {
    const resultPromise = firstValueFrom(service.buscarPorId(2));

    const req = httpTesting.expectOne(`${apiBaseUrl}/corretoras/2`);
    expect(req.request.method).toBe('GET');
    req.flush(corretora);

    expect(await resultPromise).toEqual(corretora);
  });

  it('codifica o CNPJ no segmento da busca', async () => {
    const resultPromise = firstValueFrom(service.buscarPorCnpj('12.345.678/0001-90'));

    const req = httpTesting.expectOne(`${apiBaseUrl}/corretoras/cnpj/12.345.678%2F0001-90`);
    expect(req.request.method).toBe('GET');
    req.flush(corretora);

    expect(await resultPromise).toEqual(corretora);
  });

  it('entrega erros de campo do ProblemDetail ao consumidor', () => {
    const problem: ProblemDetail = {
      status: 409,
      title: 'Conflito',
      detail: 'CNPJ já cadastrado',
      errors: { cnpj: ['CNPJ já cadastrado'] }
    };
    let received: ApiError | undefined;

    service.salvar({ cnpj: '12345678000190', cep: '01001000' }).subscribe({
      error: error => received = error
    });
    httpTesting.expectOne(`${apiBaseUrl}/corretoras`).flush(problem, {
      status: 409,
      statusText: 'Conflict'
    });

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.fieldErrors).toEqual(problem.errors!);
  });
});
