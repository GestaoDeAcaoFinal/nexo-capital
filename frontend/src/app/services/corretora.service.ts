import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../core/api/api-base-url.token';
import { Corretora, CorretoraRequest } from '../core/api/api.models';

@Injectable({
  providedIn: 'root'
})
export class CorretoraService {
  private readonly apiUrl: string;

  constructor(
    private readonly http: HttpClient,
    @Inject(API_BASE_URL) apiBaseUrl: string
  ) {
    this.apiUrl = `${apiBaseUrl.replace(/\/$/, '')}/corretoras`;
  }

  salvar(corretora: CorretoraRequest): Observable<Corretora> {
    return this.http.post<Corretora>(this.apiUrl, corretora);
  }

  listar(): Observable<Corretora[]> {
    return this.http.get<Corretora[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Corretora> {
    return this.http.get<Corretora>(`${this.apiUrl}/${id}`);
  }

  buscarPorCnpj(cnpj: string): Observable<Corretora> {
    return this.http.get<Corretora>(`${this.apiUrl}/cnpj/${encodeURIComponent(cnpj)}`);
  }
}
