package com.kowalski.casaapi.comum;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(mensagem));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> validacao(IllegalArgumentException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos na requisição");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> campoInvalido(MethodArgumentNotValidException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos na requisição"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(HandlerMethodValidationException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Dados inválidos na requisição"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroFaltando(MissingServletRequestParameterException e) {
        return erro(HttpStatus.BAD_REQUEST, "O parâmetro " + e.getParameterName() + " é obrigatório");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return erro(HttpStatus.BAD_REQUEST, "Valor inválido para " + e.getName() + ": " + e.getValue());
    }


    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErroResponse> bloqueado(LockedException e) {
        return erro(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException e) {
        return erro(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> semPermissao(AccessDeniedException e) {
        return erro(HttpStatus.FORBIDDEN, e.getMessage());
    }


    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MonitorIndisponivelException.class)
    public ResponseEntity<ErroResponse> monitorIndisponivel(MonitorIndisponivelException e) {
        log.warn("{}: {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "-");
        return erro(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e) {
        return erro(HttpStatus.NOT_FOUND, "Endereço não encontrado: /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " não suportado neste endereço");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> formatoNaoSuportado(HttpMediaTypeNotSupportedException e) {
        return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie os dados em JSON");
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e) {
        String restricao = e.getCause() instanceof ConstraintViolationException violacao ? violacao.getConstraintName() : null;
        String sqlState = e.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        log.warn("Operação recusada pelo banco: restrição {}, SQLState {}", restricao, sqlState);
        return erro(HttpStatus.CONFLICT, "Não foi possível concluir: o registro está ligado a outros dados.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e) {
        if (e instanceof ErrorResponse spring && !spring.getStatusCode().is5xxServerError()) {
            String detalhe = spring.getBody().getDetail();
            return ResponseEntity.status(spring.getStatusCode())
                    .body(new ErroResponse(detalhe != null ? detalhe : "Requisição inválida"));
        }
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado. Tente novamente.");
    }
}
