package br.org.cesjo.sgi.api;

import br.org.cesjo.sgi.exception.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.exception.RegraNegocioException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException exception) {
        Map<String, String> campos = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                LocalDateTime.now(), 400, "Revise os campos informados", campos
        ));
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ApiError> tratarRegra(RegraNegocioException exception) {
        return ResponseEntity.badRequest().body(ApiError.de(400, exception.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ApiError> tratarNaoEncontrado(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.de(404, exception.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> tratarConflito(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.de(409, "Já existe uma entrada ativa para este beneficiário"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tratarTamanho(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.de(413, "Cada documento deve ter no máximo 5 MB"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiError> tratarParteAusente(MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.de(400, "O campo " + exception.getRequestPartName() + " é obrigatório"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> tratarJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.de(400, "Os dados enviados possuem formato inválido"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> tratarErroInesperado(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.de(500, "Não foi possível concluir a operação"));
    }
}
