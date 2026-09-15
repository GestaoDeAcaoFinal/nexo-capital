import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../core/api/api-error.interceptor';
import { Corretora } from '../../core/api/api.models';
import {
  initialRequestState,
  toErrorState,
  toLoadingState,
  toSuccessState
} from '../../core/state/request-state';
import { CorretoraService } from '../../services/corretora.service';

@Component({
  selector: 'app-corretora-form',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './corretora-form.component.html',
  styleUrl: './corretora-form.component.css'
})
export class CorretoraFormComponent {
  private readonly formBuilder = inject(FormBuilder);

  readonly form = this.formBuilder.nonNullable.group({
    cnpj: ['', [
      Validators.required,
      Validators.pattern(/^\s*(?:\d{14}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})\s*$/)
    ]],
    cep: ['', [
      Validators.required,
      Validators.pattern(/^\s*(?:\d{8}|\d{5}-\d{3})\s*$/)
    ]]
  });
  state = initialRequestState<Corretora>();

  constructor(private readonly corretoraService: CorretoraService) {}

  salvar(): void {
    if (this.state.loading) {
      return;
    }

    this.clearServerErrors();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.state = toLoadingState(this.state);
    this.corretoraService.salvar(this.form.getRawValue()).subscribe({
      next: (response) => {
        this.state = toSuccessState(
          this.state,
          response,
          'Corretora cadastrada com sucesso.'
        );
      },
      error: (error: ApiError) => {
        this.applyFieldErrors(error);
        this.state = toErrorState(this.state, error);
      }
    });
  }

  private applyFieldErrors(error: ApiError): void {
    for (const [field, messages] of Object.entries(error.fieldErrors)) {
      if (field === 'cnpj' || field === 'cep') {
        const control = this.form.controls[field];
        control.setErrors({ ...control.errors, server: messages });
        control.markAsTouched();
      }
    }
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls)) {
      if (control.errors?.['server']) {
        const { server: _server, ...remainingErrors } = control.errors;
        control.setErrors(Object.keys(remainingErrors).length > 0 ? remainingErrors : null);
      }
    }
  }
}
