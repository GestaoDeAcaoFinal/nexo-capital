import { ApiError } from '../api/api-error.interceptor';
import {
  initialRequestState,
  toErrorState,
  toLoadingState,
  toSuccessState
} from './request-state';

describe('request state', () => {
  it('transiciona de inicial para loading e limpa feedback anterior', () => {
    const initial = {
      ...initialRequestState<string[]>(),
      data: ['anterior'],
      successMessage: 'Concluído',
      error: new ApiError(400, 'Erro', 'Detalhe')
    };

    expect(toLoadingState(initial)).toEqual({
      loading: true,
      data: ['anterior'],
      successMessage: null,
      error: null
    });
  });

  it('transiciona de loading para sucesso com novos dados', () => {
    const loading = toLoadingState(initialRequestState<string[]>());

    expect(toSuccessState(loading, ['novo'], 'Dados carregados')).toEqual({
      loading: false,
      data: ['novo'],
      successMessage: 'Dados carregados',
      error: null
    });
  });

  it('transiciona de loading para erro preservando os dados anteriores', () => {
    const previous = { ...initialRequestState<string[]>(), data: ['anterior'] };
    const loading = toLoadingState(previous);
    const error = new ApiError(503, 'Provedor indisponível', 'Tente novamente');

    expect(toErrorState(loading, error)).toEqual({
      loading: false,
      data: ['anterior'],
      successMessage: null,
      error
    });
  });
});
