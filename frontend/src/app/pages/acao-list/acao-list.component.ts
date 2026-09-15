import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { AcaoService } from '../../services/acao.service';

type SearchCriterion = 'id' | 'ticker';
@Component({ selector: 'app-acao-list', imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink], templateUrl: './acao-list.component.html', styleUrl: './acao-list.component.css' })
export class AcaoListComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly acaoService = inject(AcaoService);
  readonly searchForm = this.formBuilder.nonNullable.group({ criterio: this.formBuilder.nonNullable.control<SearchCriterion>('id'), valor: ['', Validators.required] });
  listState: RequestState<Acao[]> = initialRequestState<Acao[]>();
  searchState: RequestState<Acao> = initialRequestState<Acao>();
  readonly updating = new Set<number>();
  readonly updateErrors: Record<number, ApiError> = {};
  ngOnInit(): void { this.carregarAcoes(); }
  carregarAcoes(): void {
    this.listState = toLoadingState(this.listState);
    this.acaoService.listar().subscribe({ next: data => this.listState = toSuccessState(this.listState, data), error: (e: ApiError) => this.listState = toErrorState(this.listState, e) });
  }
  buscar(): void {
    const criterio = this.searchForm.controls.criterio.value; const valor = this.searchForm.controls.valor.value.trim(); this.searchForm.controls.valor.setErrors(null);
    if (!valor) { this.invalidSearch({ required: true }); return; }
    let request$: Observable<Acao>;
    if (criterio === 'id') { if (!/^[1-9]\d*$/.test(valor)) { this.invalidSearch({ positiveInteger: true }); return; } request$ = this.acaoService.buscarPorId(Number(valor)); }
    else { if (!/^[A-Za-z0-9.]{2,12}$/.test(valor)) { this.invalidSearch({ ticker: true }); return; } request$ = this.acaoService.buscarPorTicker(valor.toUpperCase()); }
    this.searchState = toLoadingState(this.searchState);
    request$.subscribe({ next: data => this.searchState = toSuccessState(this.searchState, data), error: (e: ApiError) => this.searchState = toErrorState(this.searchState, e) });
  }
  atualizarCotacao(acao: Acao): void {
    if (this.updating.has(acao.id)) return;
    this.updating.add(acao.id); delete this.updateErrors[acao.id];
    this.acaoService.atualizarCotacao(acao.id).subscribe({
      next: atualizada => { this.replaceVisible(atualizada); this.updating.delete(acao.id); },
      error: (e: ApiError) => { this.updateErrors[acao.id] = e; this.updating.delete(acao.id); }
    });
  }
  private replaceVisible(updated: Acao): void {
    if (this.listState.data) this.listState.data = this.listState.data.map(a => a.id === updated.id ? updated : a);
    if (this.searchState.data?.id === updated.id) this.searchState.data = updated;
  }
  private invalidSearch(errors: object): void { this.searchForm.controls.valor.setErrors(errors); this.searchForm.controls.valor.markAsTouched(); }
}
