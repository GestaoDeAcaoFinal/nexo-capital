import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../core/api/api-base-url.token';
import { ApiError, apiErrorInterceptor } from '../core/api/api-error.interceptor';
import { Acao, AcaoRequest, Corretora, ProblemDetail } from '../core/api/api.models';
import { AcaoService } from './acao.service';

describe('AcaoService', () => {
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
  const acao: Acao = {
    id: 1,
    ticker: 'PETR4',
    nomeEmpresa: 'Petróleo Brasileiro SA',
    mercado: 'BR',
    moeda: 'BRL',
    cotacaoAtual: 32.5,
    dataHoraCotacao: '2026-09-04T08:30:00',
    corretoraRelacionada: corretora
  };

  let service: AcaoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: apiBaseUrl }
      ]
    });
    service = TestBed.inject(AcaoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('envia somente o contrato de cadastro para POST /acoes', async () => {
    const request: AcaoRequest = { ticker: 'PETR4', corretoraId: 2 };
    const resultPromise = firstValueFrom(service.salvar(request));

    const req = httpTesting.expectOne(`${apiBaseUrl}/acoes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush(acao);

    expect(await resultPromise).toEqual(acao);
  });

  it('lista ações com GET /acoes', async () => {
    const resultPromise = firstValueFrom(service.listar());

    const req = httpTesting.expectOne(`${apiBaseUrl}/acoes`);
    expect(req.request.method).toBe('GET');
    req.flush([acao]);

    expect(await resultPromise).toEqual([acao]);
  });

  it('busca ação por id com GET /acoes/{id}', async () => {
    const resultPromise = firstValueFrom(service.buscarPorId(1));

    const req = httpTesting.expectOne(`${apiBaseUrl}/acoes/1`);
    expect(req.request.method).toBe('GET');
    req.flush(acao);

    expect(await resultPromise).toEqual(acao);
  });

  it('codifica o ticker na busca por ticker', async () => {
    const resultPromise = firstValueFrom(service.buscarPorTicker('A/B'));

    const req = httpTesting.expectOne(`${apiBaseUrl}/acoes/ticker/A%2FB`);
    expect(req.request.method).toBe('GET');
    req.flush(acao);

    expect(await resultPromise).toEqual(acao);
  });

  it('atualiza cotação com PUT e corpo vazio', async () => {
    const resultPromise = firstValueFrom(service.atualizarCotacao(1));

    const req = httpTesting.expectOne(`${apiBaseUrl}/acoes/1/atualizar-cotacao`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush(acao);

    expect(await resultPromise).toEqual(acao);
  });

  it('entrega ProblemDetail padronizado ao consumidor', () => {
    const problem: ProblemDetail = {
      status: 422,
      title: 'Dado externo não encontrado',
      detail: 'Ticker não encontrado'
    };
    let received: ApiError | undefined;

    service.buscarPorTicker('INVALID').subscribe({ error: error => received = error });
    httpTesting.expectOne(`${apiBaseUrl}/acoes/ticker/INVALID`).flush(problem, {
      status: 422,
      statusText: 'Unprocessable Content'
    });

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.message).toBe('Ticker não encontrado');
  });
});
