import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiError, apiErrorInterceptor } from './api-error.interceptor';
import { ProblemDetail } from './api.models';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  [400, 404, 409, 422, 502, 503].forEach(status => {
    it(`preserva ProblemDetail e erros por campo para status ${status}`, () => {
      const problem: ProblemDetail = {
        status,
        title: `Título ${status}`,
        detail: `Detalhe ${status}`,
        errors: { ticker: ['Ticker inválido'] }
      };
      let received: ApiError | undefined;

      http.get('/resource').subscribe({ error: error => received = error });
      httpTesting.expectOne('/resource').flush(problem, { status, statusText: 'Erro' });

      expect(received).toEqual(jasmine.any(ApiError));
      expect(received?.status).toBe(status);
      expect(received?.title).toBe(problem.title);
      expect(received?.message).toBe(problem.detail);
      expect(received?.fieldErrors).toEqual(problem.errors!);
    });
  });

  it('produz fallback estável quando a API está inacessível', () => {
    let received: ApiError | undefined;

    http.get('/resource').subscribe({ error: error => received = error });
    httpTesting.expectOne('/resource').error(new ProgressEvent('error'));

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.status).toBe(0);
    expect(received?.title).toBe('Falha de conexão');
    expect(received?.message).toBe('Não foi possível conectar à API. Tente novamente.');
    expect(received?.fieldErrors).toEqual({});
  });

  it('produz fallback estável para corpo de erro incompatível', () => {
    let received: ApiError | undefined;

    http.get('/resource').subscribe({ error: error => received = error });
    httpTesting.expectOne('/resource').flush('resposta inválida', {
      status: 500,
      statusText: 'Internal Server Error'
    });

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.status).toBe(500);
    expect(received?.title).toBe('Erro inesperado');
    expect(received?.message).toBe('Não foi possível concluir a operação. Tente novamente.');
  });
});
