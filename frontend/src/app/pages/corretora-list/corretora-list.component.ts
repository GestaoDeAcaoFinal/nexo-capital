import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ApiError } from '../../core/api/api-error.interceptor';
import { Corretora } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { CorretoraService } from '../../services/corretora.service';

type SearchCriterion = 'id' | 'cnpj';

@Component({
  selector: 'app-corretora-list',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './corretora-list.component.html',
  styleUrl: './corretora-list.component.css'
})
export class CorretoraListComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly corretoraService = inject(CorretoraService);
  readonly searchForm = this.formBuilder.nonNullable.group({
    criterio: this.formBuilder.nonNullable.control<SearchCriterion>('id'),
    valor: ['', Validators.required]
  });
  listState: RequestState<Corretora[]> = initialRequestState<Corretora[]>();
  searchState: RequestState<Corretora> = initialRequestState<Corretora>();

  ngOnInit(): void {
    this.carregarCorretoras();
  }

  carregarCorretoras(): void {
    this.listState = toLoadingState(this.listState);
    this.corretoraService.listar().subscribe({
      next: data => this.listState = toSuccessState(this.listState, data),
      error: (error: ApiError) => this.listState = toErrorState(this.listState, error)
    });
  }

  buscar(): void {
    const criterion = this.searchForm.controls.criterio.value;
    const value = this.searchForm.controls.valor.value.trim();
    this.searchForm.controls.valor.setErrors(null);
    if (!value) {
      this.searchForm.controls.valor.setErrors({ required: true });
      this.searchForm.controls.valor.markAsTouched();
      return;
    }

    let request$: Observable<Corretora>;
    if (criterion === 'id') {
      if (!/^[1-9]\d*$/.test(value)) {
        this.searchForm.controls.valor.setErrors({ positiveInteger: true });
        this.searchForm.controls.valor.markAsTouched();
        return;
      }
      request$ = this.corretoraService.buscarPorId(Number(value));
    } else {
      if (!/^\s*(?:\d{14}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})\s*$/.test(value)) {
        this.searchForm.controls.valor.setErrors({ cnpj: true });
        this.searchForm.controls.valor.markAsTouched();
        return;
      }
      request$ = this.corretoraService.buscarPorCnpj(value);
    }
    this.searchState = toLoadingState(this.searchState);
    request$.subscribe({
      next: data => this.searchState = toSuccessState(this.searchState, data),
      error: (error: ApiError) => this.searchState = toErrorState(this.searchState, error)
    });
  }
}
