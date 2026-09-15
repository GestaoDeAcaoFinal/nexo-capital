export interface CorretoraRequest {
  cnpj: string;
  cep: string;
}

export interface AcaoRequest {
  ticker: string;
  corretoraId: number;
}

export interface Corretora {
  id: number;
  cnpj: string;
  razaoSocial: string | null;
  nomeFantasia: string | null;
  email: string | null;
  telefone: string | null;
  cep: string;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  situacaoCadastral: string | null;
  validadaNaCvm: boolean | null;
  dataCadastro: string | null;
}

export interface Acao {
  id: number;
  ticker: string;
  nomeEmpresa: string | null;
  mercado: string | null;
  moeda: string | null;
  cotacaoAtual: number | null;
  dataHoraCotacao: string | null;
  corretoraRelacionada: Corretora;
}

export type TipoOperacao = 'COMPRA' | 'VENDA';

export interface OperacaoAcao {
  id: number;
  quantidade: number;
  precoUnitario: number;
  precoMedio: number | null;
  lucroPrejuizo: number | null;
  dataOperacao: string;
  valorTotal: number;
  tipoOperacao: TipoOperacao;
  acao: Acao;
}

export interface PosicaoCarteira {
  acaoId: number;
  ticker: string;
  quantidadeAtual: number;
  precoMedio: number;
  custoTotal: number;
  cotacaoAtual: number;
  valorAtual: number;
}

export interface ResumoCarteira {
  posicoes: PosicaoCarteira[];
  quantidadeTotal: number;
  precoMedioCarteira: number;
  custoTotalCarteira: number;
  valorAtualCarteira: number;
}

export type FieldErrors = Record<string, string[]>;

export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  errors?: FieldErrors;
}
