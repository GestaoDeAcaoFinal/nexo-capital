import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { environment } from '../environments/environment';
import { API_BASE_URL } from './core/api/api-base-url.token';
import { ApiError } from './core/api/api-error.interceptor';
import { appConfig } from './app.config';

describe('appConfig', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ...(appConfig.providers ?? []),
        provideHttpClientTesting()
      ]
    });
  });

  it('fornece a URL base configurada pelo ambiente', () => {
    expect(TestBed.inject(API_BASE_URL)).toBe(environment.apiBaseUrl);
    expect(environment.apiBaseUrl).toBe('http://localhost:8080');
  });

  it('configura o locale pt-BR para datas e valores monetários', () => {
    const locale = TestBed.inject(LOCALE_ID);

    expect(locale).toBe('pt-BR');
    expect(new CurrencyPipe(locale).transform(1234.5, 'BRL')).toContain('1.234,50');
    expect(new DatePipe(locale).transform('2026-09-04T10:00:00', 'shortDate'))
      .toBe('04/09/2026');
  });

  it('registra o interceptor de erro sem adicionar autenticação', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    let received: ApiError | undefined;

    http.get('/resource').subscribe({ error: error => received = error });
    const request = httpTesting.expectOne('/resource');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush(
      { status: 404, title: 'Recurso não encontrado', detail: 'Ação não encontrada' },
      { status: 404, statusText: 'Not Found' }
    );

    expect(received).toEqual(jasmine.any(ApiError));
    expect(received?.message).toBe('Ação não encontrada');
    httpTesting.verify();
  });
});
