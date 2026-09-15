import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/api/api-error.interceptor';
import { OperacaoAcao, PosicaoCarteira, ResumoCarteira } from '../../core/api/api.models';
import {
  initialRequestState,
  RequestState,
  toErrorState,
  toLoadingState,
  toSuccessState
} from '../../core/state/request-state';
import { OperacaoService } from '../../services/operacao.service';

@Component({
  selector: 'app-home',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css', './home-dashboard.css']
})
export class HomeComponent implements OnInit {
  private readonly operacaoService = inject(OperacaoService);

  carteiraState: RequestState<ResumoCarteira> = initialRequestState<ResumoCarteira>();
  operacoesState: RequestState<OperacaoAcao[]> = initialRequestState<OperacaoAcao[]>();

  ngOnInit(): void {
    this.carregarDashboard();
  }

  carregarDashboard(): void {
    this.carregarCarteira();
    this.carregarOperacoes();
  }

  get resultadoCarteira(): number {
    const carteira = this.carteiraState.data;
    return carteira ? carteira.valorAtualCarteira - carteira.custoTotalCarteira : 0;
  }

  get posicoesRelevantes(): PosicaoCarteira[] {
    return [...(this.carteiraState.data?.posicoes ?? [])]
      .sort((first, second) => second.valorAtual - first.valorAtual)
      .slice(0, 5);
  }

  get operacoesRecentes(): OperacaoAcao[] {
    return [...(this.operacoesState.data ?? [])]
      .sort((first, second) => new Date(second.dataOperacao).getTime() - new Date(first.dataOperacao).getTime())
      .slice(0, 5);
  }

  allocationPercent(posicao: PosicaoCarteira): number {
    const total = this.carteiraState.data?.valorAtualCarteira ?? 0;
    return total > 0 ? Math.round((posicao.valorAtual / total) * 10000) / 100 : 0;
  }

  private carregarCarteira(): void {
    this.carteiraState = toLoadingState(this.carteiraState);
    this.operacaoService.resumirCarteira().subscribe({
      next: data => this.carteiraState = toSuccessState(this.carteiraState, data),
      error: (error: ApiError) => this.carteiraState = toErrorState(this.carteiraState, error)
    });
  }

  private carregarOperacoes(): void {
    this.operacoesState = toLoadingState(this.operacoesState);
    this.operacaoService.listar().subscribe({
      next: data => this.operacoesState = toSuccessState(this.operacoesState, data),
      error: (error: ApiError) => this.operacoesState = toErrorState(this.operacoesState, error)
    });
  }
}
