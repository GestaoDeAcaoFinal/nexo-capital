import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../core/api/api-base-url.token';
import { Acao, AcaoRequest } from '../core/api/api.models';

@Injectable({
  providedIn: 'root'
})
export class AcaoService {
  private readonly apiUrl: string;

  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) apiBaseUrl: string
  ) {
    this.apiUrl = `${apiBaseUrl.replace(/\/$/, '')}/acoes`;
  }

  salvar(acao: AcaoRequest): Observable<Acao> {
    return this.http.post<Acao>(this.apiUrl, acao);
  }

  listar(): Observable<Acao[]> {
    return this.http.get<Acao[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Acao> {
    return this.http.get<Acao>(`${this.apiUrl}/${id}`);
  }

  buscarPorTicker(ticker: string): Observable<Acao> {
    return this.http.get<Acao>(`${this.apiUrl}/ticker/${encodeURIComponent(ticker)}`);
  }

  atualizarCotacao(id: number): Observable<Acao> {
    return this.http.put<Acao>(`${this.apiUrl}/${id}/atualizar-cotacao`, {});
  }
}
