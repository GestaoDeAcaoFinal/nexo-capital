import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, OperacaoAcao } from '../../core/api/api.models';
import {
  initialRequestState,
  RequestState,
  toErrorState,
  toLoadingState,
  toSuccessState
} from '../../core/state/request-state';
import { AcaoService } from '../../services/acao.service';
import { OperacaoService } from '../../services/operacao.service';

type OperationFilter = 'todas' | 'compras' | 'vendas' | 'historico';

@Component({
  selector: 'app-operacao-list',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './operacao-list.component.html',
  styleUrl: './operacao-list.component.css'
})
export class OperacaoListComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly operacaoService = inject(OperacaoService);
  private readonly acaoService = inject(AcaoService);

  readonly historicoForm = this.formBuilder.nonNullable.group({
    acaoId: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(1)])
  });
  readonly idForm = this.formBuilder.nonNullable.group({
    id: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(1)])
  });

  filtroAtivo: OperationFilter = 'todas';
  listState: RequestState<OperacaoAcao[]> = initialRequestState<OperacaoAcao[]>();
  detailState: RequestState<OperacaoAcao> = initialRequestState<OperacaoAcao>();
  acoesState: RequestState<Acao[]> = initialRequestState<Acao[]>();
  private listRequestGeneration = 0;

  ngOnInit(): void {
    this.carregarAcoes();
    this.carregar();
  }

  carregar(): void {
    if (this.filtroAtivo === 'historico') {
      this.buscarHistorico();
      return;
    }

    this.loadList(this.requestFor(this.filtroAtivo));
  }

  aplicarFiltro(filtro: Exclude<OperationFilter, 'historico'>): void {
    this.filtroAtivo = filtro;
    this.loadList(this.requestFor(filtro));
  }

  buscarHistorico(): void {
    if (this.historicoForm.invalid) {
      this.historicoForm.markAllAsTouched();
      return;
    }

    this.filtroAtivo = 'historico';
    this.loadList(this.operacaoService.listarHistorico(this.historicoForm.getRawValue().acaoId));
  }

  buscarPorId(): void {
    if (this.detailState.loading) return;
    if (this.idForm.invalid) {
      this.idForm.markAllAsTouched();
      return;
    }

    this.detailState = toLoadingState(this.detailState);
    this.operacaoService.buscarPorId(this.idForm.getRawValue().id).subscribe({
      next: data => this.detailState = toSuccessState(this.detailState, data),
      error: (error: ApiError) => this.detailState = toErrorState(this.detailState, error)
    });
  }

  carregarAcoes(): void {
    this.acoesState = toLoadingState(this.acoesState);
    this.acaoService.listar().subscribe({
      next: data => this.acoesState = toSuccessState(this.acoesState, data),
      error: (error: ApiError) => this.acoesState = toErrorState(this.acoesState, error)
    });
  }

  private requestFor(filtro: Exclude<OperationFilter, 'historico'>): Observable<OperacaoAcao[]> {
    switch (filtro) {
      case 'compras': return this.operacaoService.listarCompras();
      case 'vendas': return this.operacaoService.listarVendas();
      default: return this.operacaoService.listar();
    }
  }

  private loadList(request$: Observable<OperacaoAcao[]>): void {
    const requestGeneration = ++this.listRequestGeneration;
    this.listState = toLoadingState(this.listState);
    request$.subscribe({
      next: data => {
        if (requestGeneration === this.listRequestGeneration) {
          this.listState = toSuccessState(this.listState, data);
        }
      },
      error: (error: ApiError) => {
        if (requestGeneration === this.listRequestGeneration) {
          this.listState = toErrorState(this.listState, error);
        }
      }
    });
  }
}
