package com.av2.gestaodeacao.resources.exceptions;

import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntradaOuOperacaoInvalidaException.class)
    public ProblemDetail handleEntradaInvalida(EntradaOuOperacaoInvalidaException exception) {
        return criarProblema(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail handleRecursoNaoEncontrado(RecursoNaoEncontradoException exception) {
        return criarProblema(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail handleConflito(ConflitoException exception) {
        return criarProblema(HttpStatus.CONFLICT, "Conflito", exception.getMessage());
    }

    @ExceptionHandler(DadoExternoNaoEncontradoException.class)
    public ProblemDetail handleDadoExterno(DadoExternoNaoEncontradoException exception) {
        return criarProblema(HttpStatus.UNPROCESSABLE_CONTENT, "Dado externo não encontrado", exception.getMessage());
    }

    @ExceptionHandler(FalhaDeProvedorException.class)
    public ProblemDetail handleFalhaDeProvedor(FalhaDeProvedorException exception) {
        return criarProblema(HttpStatus.BAD_GATEWAY, "Falha no provedor", exception.getMessage());
    }

    @ExceptionHandler(ProvedorIndisponivelException.class)
    public ProblemDetail handleProvedorIndisponivel(ProvedorIndisponivelException exception) {
        return criarProblema(HttpStatus.SERVICE_UNAVAILABLE, "Provedor indisponível", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleCamposInvalidos(MethodArgumentNotValidException exception) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            adicionarErro(erros, fieldError.getField(), fieldError.getDefaultMessage());
        }
        return criarProblemaDeValidacao(erros);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParametrosInvalidos(HandlerMethodValidationException exception) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String nome = result.getMethodParameter().getParameterName();
            if (nome == null) {
                nome = "parametro";
            }
            for (var erro : result.getResolvableErrors()) {
                adicionarErro(erros, nome, erro.getDefaultMessage());
            }
        }
        return criarProblemaDeValidacao(erros);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleViolacoes(ConstraintViolationException exception) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String caminho = violation.getPropertyPath().toString();
            String nome = caminho.substring(caminho.lastIndexOf('.') + 1);
            adicionarErro(erros, nome, violation.getMessage());
        }
        return criarProblemaDeValidacao(erros);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleRequisicaoMalformada(Exception exception) {
        return criarProblema(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "A requisição está malformada ou possui parâmetros ausentes ou inválidos"
        );
    }

    private ProblemDetail criarProblemaDeValidacao(Map<String, List<String>> erros) {
        ProblemDetail problem = criarProblema(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Um ou mais campos da requisição são inválidos"
        );
        problem.setProperty("errors", erros);
        return problem;
    }

    private ProblemDetail criarProblema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(titulo);
        return problem;
    }

    private void adicionarErro(Map<String, List<String>> erros, String campo, String mensagem) {
        erros.computeIfAbsent(campo, chave -> new ArrayList<>())
                .add(mensagem == null ? "Valor inválido" : mensagem);
    }
}
