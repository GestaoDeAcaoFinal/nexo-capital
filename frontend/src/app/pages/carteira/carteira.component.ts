import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiError } from '../../core/api/api-error.interceptor';
import { ResumoCarteira } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { OperacaoService } from '../../services/operacao.service';

@Component({
  selector: 'app-carteira',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './carteira.component.html',
  styleUrl: './carteira.component.css'
})
export class CarteiraComponent implements OnInit {
  private readonly operacaoService = inject(OperacaoService);
  carteiraState: RequestState<ResumoCarteira> = initialRequestState<ResumoCarteira>();

  ngOnInit(): void {
    this.carregarCarteira();
  }

  get resultadoCarteira(): number {
    const carteira = this.carteiraState.data;
    return carteira ? carteira.valorAtualCarteira - carteira.custoTotalCarteira : 0;
  }

  carregarCarteira(): void {
    this.carteiraState = toLoadingState(this.carteiraState);
    this.operacaoService.resumirCarteira().subscribe({
      next: data => this.carteiraState = toSuccessState(this.carteiraState, data),
      error: (error: ApiError) => this.carteiraState = toErrorState(this.carteiraState, error)
    });
  }
}
