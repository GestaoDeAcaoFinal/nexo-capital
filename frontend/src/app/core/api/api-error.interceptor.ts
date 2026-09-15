import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

import { FieldErrors, ProblemDetail } from './api.models';

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly title: string,
    detail: string,
    readonly fieldErrors: FieldErrors = {}
  ) {
    super(detail);
    this.name = 'ApiError';
  }
}

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => throwError(() => normalizeApiError(error)))
  );

export function normalizeApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return unexpectedError();
  }

  if (error.status === 0) {
    return new ApiError(
      0,
      'Falha de conexão',
      'Não foi possível conectar à API. Tente novamente.'
    );
  }

  if (isProblemDetail(error.error)) {
    return new ApiError(
      error.error.status,
      error.error.title,
      error.error.detail,
      error.error.errors ?? {}
    );
  }

  return unexpectedError(error.status);
}

function unexpectedError(status = 0): ApiError {
  return new ApiError(
    status,
    'Erro inesperado',
    'Não foi possível concluir a operação. Tente novamente.'
  );
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!isRecord(value)) {
    return false;
  }

  return typeof value['status'] === 'number'
    && typeof value['title'] === 'string'
    && typeof value['detail'] === 'string'
    && (value['errors'] === undefined || isFieldErrors(value['errors']));
}

function isFieldErrors(value: unknown): value is FieldErrors {
  return isRecord(value)
    && Object.values(value).every(messages =>
      Array.isArray(messages) && messages.every(message => typeof message === 'string')
    );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
