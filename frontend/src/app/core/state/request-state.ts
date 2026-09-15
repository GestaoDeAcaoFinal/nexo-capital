import { ApiError } from '../api/api-error.interceptor';

export interface RequestState<T> {
  loading: boolean;
  data: T | null;
  successMessage: string | null;
  error: ApiError | null;
}

export function initialRequestState<T>(): RequestState<T> {
  return {
    loading: false,
    data: null,
    successMessage: null,
    error: null
  };
}

export function toLoadingState<T>(state: RequestState<T>): RequestState<T> {
  return {
    loading: true,
    data: state.data,
    successMessage: null,
    error: null
  };
}

export function toSuccessState<T>(
  _state: RequestState<T>,
  data: T,
  successMessage: string | null = null
): RequestState<T> {
  return {
    loading: false,
    data,
    successMessage,
    error: null
  };
}

export function toErrorState<T>(state: RequestState<T>, error: ApiError): RequestState<T> {
  return {
    loading: false,
    data: state.data,
    successMessage: null,
    error
  };
}
