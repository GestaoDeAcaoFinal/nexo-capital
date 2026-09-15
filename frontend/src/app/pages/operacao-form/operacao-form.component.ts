import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiError } from '../../core/api/api-error.interceptor';
import { Acao, OperacaoAcao, TipoOperacao } from '../../core/api/api.models';
import { initialRequestState, RequestState, toErrorState, toLoadingState, toSuccessState } from '../../core/state/request-state';
import { AcaoService } from '../../services/acao.service';
import { OperacaoService } from '../../services/operacao.service';

@Component({ selector: 'app-operacao-form', imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink], templateUrl: './operacao-form.component.html', styleUrl: './operacao-form.component.css' })
export class OperacaoFormComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder); private readonly operacaoService = inject(OperacaoService); private readonly acaoService = inject(AcaoService);
  readonly form = this.formBuilder.group({
    acaoId: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(1)]),
    quantidade: this.formBuilder.nonNullable.control(0, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]),
    tipoOperacao: this.formBuilder.nonNullable.control<TipoOperacao>('COMPRA'),
    precoCompra: this.formBuilder.control<number | null>(null),
    precoVenda: this.formBuilder.control<number | null>(null)
  });
  state: RequestState<OperacaoAcao> = initialRequestState<OperacaoAcao>(); acoesState: RequestState<Acao[]> = initialRequestState<Acao[]>();
  get acaoSelecionada(): Acao | null {
    const acaoId = this.form.controls.acaoId.value;
    return this.acoesState.data?.find(acao => acao.id === acaoId) ?? null;
  }
  get moedaSelecionada(): string {
    return this.acaoSelecionada?.moeda || 'BRL';
  }
  ngOnInit(): void {
    this.carregarAcoes();
    this.form.controls.tipoOperacao.valueChanges.subscribe(() => this.updatePriceValidation());
    this.form.controls.acaoId.valueChanges.subscribe(acaoId => this.preencherPrecoCompra(acaoId));
    this.updatePriceValidation();
  }
  carregarAcoes(): void {
    this.acoesState = toLoadingState(this.acoesState);
    this.acaoService.listar().subscribe({
      next: data => {
        this.acoesState = toSuccessState(this.acoesState, data);
        this.preencherPrecoCompra(this.form.controls.acaoId.value);
      },
      error: (e: ApiError) => this.acoesState = toErrorState(this.acoesState, e)
    });
  }
  salvar(): void {
    if (this.state.loading) return; this.updatePriceValidation();
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const { acaoId, quantidade, tipoOperacao, precoCompra, precoVenda } = this.form.getRawValue();
    let request$: Observable<OperacaoAcao>;
    if (tipoOperacao === 'COMPRA') request$ = this.operacaoService.comprar(acaoId, quantidade, precoCompra!);
    else request$ = this.operacaoService.vender(acaoId, quantidade, precoVenda!);
    this.state = toLoadingState(this.state);
    request$.subscribe({ next: data => this.state = toSuccessState(this.state, data, `${tipoOperacao === 'COMPRA' ? 'Compra' : 'Venda'} realizada com sucesso.`), error: (e: ApiError) => this.state = toErrorState(this.state, e) });
  }
  private updatePriceValidation(): void {
    const compra = this.form.controls.precoCompra;
    const venda = this.form.controls.precoVenda;
    const ehCompra = this.form.controls.tipoOperacao.value === 'COMPRA';
    compra.setValidators(ehCompra ? [Validators.required, Validators.min(0.01)] : []);
    venda.setValidators(ehCompra ? [] : [Validators.required, Validators.min(0.01)]);
    compra.updateValueAndValidity({ emitEvent: false });
    venda.updateValueAndValidity({ emitEvent: false });
  }
  private preencherPrecoCompra(acaoId: number): void {
    const acao = this.acoesState.data?.find(item => item.id === acaoId);
    this.form.controls.precoCompra.setValue(acao?.cotacaoAtual ?? null, { emitEvent: false });
  }
}
