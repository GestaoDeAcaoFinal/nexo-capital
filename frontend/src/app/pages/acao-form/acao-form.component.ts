import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, Corretora } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { AcaoService } from '../../services/acao.service';
import { CorretoraService } from '../../services/corretora.service';

@Component({ selector: 'app-acao-form', imports: [CurrencyPipe, RouterLink, ReactiveFormsModule], templateUrl: './acao-form.component.html', styleUrl: './acao-form.component.css' })
export class AcaoFormComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly acaoService = inject(AcaoService);
  private readonly corretoraService = inject(CorretoraService);
  readonly form = this.formBuilder.nonNullable.group({
    ticker: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9.]{2,12}$/)]],
    corretoraId: [0, [Validators.required, Validators.min(1)]]
  });
  state: RequestState<Acao> = initialRequestState<Acao>();
  corretorasState: RequestState<Corretora[]> = initialRequestState<Corretora[]>();

  ngOnInit(): void { this.carregarCorretoras(); }
  carregarCorretoras(): void {
    this.corretorasState = toLoadingState(this.corretorasState);
    this.corretoraService.listar().subscribe({
      next: data => this.corretorasState = toSuccessState(this.corretorasState, data),
      error: (error: ApiError) => this.corretorasState = toErrorState(this.corretorasState, error)
    });
  }
  salvar(): void {
    if (this.state.loading) return;
    this.clearServerErrors();
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const request = { ticker: raw.ticker.trim().toUpperCase(), corretoraId: raw.corretoraId };
    this.state = toLoadingState(this.state);
    this.acaoService.salvar(request).subscribe({
      next: data => this.state = toSuccessState(this.state, data, 'Ação cadastrada com sucesso.'),
      error: (error: ApiError) => { this.applyFieldErrors(error); this.state = toErrorState(this.state, error); }
    });
  }
  private applyFieldErrors(error: ApiError): void {
    for (const [field, messages] of Object.entries(error.fieldErrors)) {
      if (field === 'ticker' || field === 'corretoraId') this.form.controls[field].setErrors({ ...this.form.controls[field].errors, server: messages });
    }
  }
  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls)) if (control.errors?.['server']) { const { server: _, ...other } = control.errors; control.setErrors(Object.keys(other).length ? other : null); }
  }
}
