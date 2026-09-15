import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '../core/api/api-base-url.token';
import { ApiError, apiErrorInterceptor } from '../core/api/api-error.interceptor';
import { Acao, Corretora, OperacaoAcao, ResumoCarteira } from '../core/api/api.models';
import { OperacaoService } from './operacao.service';

describe('OperacaoService', () => {
  const apiBaseUrl = 'https://api.example.test';
  const corretora: Corretora = {
    id: 2, cnpj: '12345678000190', razaoSocial: 'Corretora Teste SA',
    nomeFantasia: 'Corretora Teste', email: null, telefone: null, cep: '01001000',
    logradouro: null, numero: null, complemento: null, bairro: null, cidade: 'São Paulo',
    uf: 'SP', situacaoCadastral: 'ATIVA', validadaNaCvm: true,
    dataCadastro: '2026-09-04T08:00:00'
  };
  const acao: Acao = {
    id: 1, ticker: 'PETR4', nomeEmpresa: 'Petróleo Brasileiro SA', mercado: 'BR',
    moeda: 'BRL', cotacaoAtual: 32.5, dataHoraCotacao: '2026-09-04T08:30:00',
    corretoraRelacionada: corretora
  };
  const operacao: OperacaoAcao = {
    id: 3,
    quantidade: 10,
    precoUnitario: 32.5,
    precoMedio: null,
    lucroPrejuizo: null,
    dataOperacao: '2026-09-04T09:00:00',
    valorTotal: 325,
    tipoOperacao: 'COMPRA',
    acao
  };

  let service: OperacaoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: apiBaseUrl }
      ]
    });
    service = TestBed.inject(OperacaoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('consulta o resumo atual da carteira', async () => {
    const resumo: ResumoCarteira = {
      posicoes: [], quantidadeTotal: 0, precoMedioCarteira: 0,
      custoTotalCarteira: 0, valorAtualCarteira: 0
    };
    const resultPromise = firstValueFrom(service.resumirCarteira());
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/carteira`);
    expect(req.request.method).toBe('GET');
    req.flush(resumo);
    expect(await resultPromise).toEqual(resumo);
  });

  it('envia o preço de compra editado', async () => {
    const resultPromise = firstValueFrom(service.comprar(1, 1, 30));
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/comprar/1?quantidade=1&precoCompra=30`);

    expect(req.request.method).toBe('POST');
    req.flush({ ...operacao, quantidade: 1, precoUnitario: 30, precoMedio: 30, valorTotal: 30 });

    expect((await resultPromise).precoUnitario).toBe(30);
  });

  it('lista todas as operações', async () => {
    const resultPromise = firstValueFrom(service.listar());
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes`);
    expect(req.request.method).toBe('GET');
    req.flush([operacao]);
    expect(await resultPromise).toEqual([operacao]);
  });

  it('registra compra com quantidade como query parameter e corpo vazio', async () => {
    const resultPromise = firstValueFrom(service.comprar(1, 10, 32.5));
    const req = httpTesting.expectOne(request => request.url === `${apiBaseUrl}/operacoes/comprar/1`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('precoCompra')).toBe('32.5');
    expect(req.request.params.get('quantidade')).toBe('10');
    expect(req.request.body).toEqual({});
    req.flush(operacao);
    expect(await resultPromise).toEqual(operacao);
  });

  it('registra venda com quantidade e preço como query parameters', async () => {
    const venda = { ...operacao, tipoOperacao: 'VENDA' as const, precoUnitario: 40, lucroPrejuizo: 75 };
    const resultPromise = firstValueFrom(service.vender(1, 10, 40));
    const req = httpTesting.expectOne(request => request.url === `${apiBaseUrl}/operacoes/vender/1`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('quantidade')).toBe('10');
    expect(req.request.params.get('precoVenda')).toBe('40');
    expect(req.request.body).toEqual({});
    req.flush(venda);
    expect(await resultPromise).toEqual(venda);
  });

  it('lista histórico por ação', async () => {
    const resultPromise = firstValueFrom(service.listarHistorico(1));
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/1`);
    expect(req.request.method).toBe('GET');
    req.flush([operacao]);
    expect(await resultPromise).toEqual([operacao]);
  });

  it('lista somente compras', async () => {
    const resultPromise = firstValueFrom(service.listarCompras());
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/compras`);
    expect(req.request.method).toBe('GET');
    req.flush([operacao]);
    expect(await resultPromise).toEqual([operacao]);
  });

  it('lista somente vendas', async () => {
    const resultPromise = firstValueFrom(service.listarVendas());
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/vendas`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
    expect(await resultPromise).toEqual([]);
  });

  it('busca operação por id no caminho /buscar/{id}', async () => {
    const resultPromise = firstValueFrom(service.buscarPorId(3));
    const req = httpTesting.expectOne(`${apiBaseUrl}/operacoes/buscar/3`);
    expect(req.request.method).toBe('GET');
    req.flush(operacao);
    expect(await resultPromise).toEqual(operacao);
  });

  it('entrega fallback de rede padronizado ao consumidor', () => {
    let received: ApiError | undefined;

    service.listar().subscribe({ error: error => received = error });
    httpTesting.expectOne(`${apiBaseUrl}/operacoes`).error(new ProgressEvent('error'));

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.status).toBe(0);
    expect(received?.title).toBe('Falha de conexão');
  });
});
