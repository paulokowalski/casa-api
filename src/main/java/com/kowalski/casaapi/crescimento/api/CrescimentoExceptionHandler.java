package com.kowalski.casaapi.crescimento.api;

import com.kowalski.casaapi.crescimento.comum.ArmazenamentoIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.FotoInvalidaException;
import com.kowalski.casaapi.crescimento.comum.IaIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Erros próprios do módulo. Tem precedência sobre o tratador geral da aplicação, que continua cuidando dos
 * erros comuns (validação, JSON inválido, login).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CrescimentoExceptionHandler {

    private static ResponseEntity<ErroCrescimento> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroCrescimento(mensagem));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroCrescimento> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SemPermissaoException.class)
    public ResponseEntity<ErroCrescimento> semPermissao(SemPermissaoException e) {
        return erro(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(FotoInvalidaException.class)
    public ResponseEntity<ErroCrescimento> fotoInvalida(FotoInvalidaException e) {
        return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroCrescimento> grandeDemais(MaxUploadSizeExceededException e) {
        return erro(HttpStatus.CONTENT_TOO_LARGE, "A foto é grande demais. O limite é de 25 MB por foto.");
    }

    @ExceptionHandler(ArmazenamentoIndisponivelException.class)
    public ResponseEntity<ErroCrescimento> armazenamento(ArmazenamentoIndisponivelException e) {
        log.error("{}", e.getMessage(), e.getCause());
        return erro(HttpStatus.SERVICE_UNAVAILABLE, "As fotos estão indisponíveis agora. Tente de novo em alguns minutos.");
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<ErroCrescimento> ia(IaIndisponivelException e) {
        if (e.getCause() != null) log.warn("IA indisponível: {}", e.getCause().toString());
        return erro(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
