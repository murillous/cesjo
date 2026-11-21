package br.org.cesjo.sgi.compartilhado.web;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte as exceções da API em respostas {@link ProblemDetail} (RFC 9457).
 *
 * <p>Estende {@link ResponseEntityExceptionHandler}, que já trata as exceções padrão do Spring MVC. Os
 * títulos e detalhes dessas exceções estão em português em {@code messages.properties}.
 */
@RestControllerAdvice
public class TratadorGlobalErros extends ResponseEntityExceptionHandler {

	/** Propriedade do {@link ProblemDetail} com a lista de campos inválidos. */
	public static final String PROPRIEDADE_ERROS = "erros";

	private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErros.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
	}

	@ExceptionHandler(RegraNegocioException.class)
	ProblemDetail tratarRegraNegocio(RegraNegocioException ex) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", ex.getMessage());
	}

	/**
	 * Falhas de autenticação e de permissão voltam para o Spring Security, que responde 401 e 403. Sem
	 * isto, o tratador genérico abaixo as transformaria em 500.
	 */
	@ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
	void repassarParaSpringSecurity(RuntimeException ex) {
		throw ex;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail tratarErroInesperado(Exception ex) {
		log.error("Erro inesperado ao processar a requisição", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
				"Ocorreu um erro inesperado. Tente novamente mais tarde.");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<CampoInvalido> erros = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
			.sorted(Comparator.comparing(CampoInvalido::campo).thenComparing(CampoInvalido::mensagem))
			.toList();
		return validacao(headers, status, erros);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<CampoInvalido> erros = ex.getParameterValidationResults()
			.stream()
			.flatMap(resultado -> resultado.getResolvableErrors()
				.stream()
				.map(erro -> new CampoInvalido(
						erro instanceof FieldError campo ? campo.getField()
								: resultado.getMethodParameter().getParameterName(),
						erro.getDefaultMessage())))
			.sorted(Comparator.comparing(CampoInvalido::campo).thenComparing(CampoInvalido::mensagem))
			.toList();
		return validacao(headers, status, erros);
	}

	private ResponseEntity<Object> validacao(HttpHeaders headers, HttpStatusCode status, List<CampoInvalido> erros) {
		ProblemDetail problema = problema(status, "Dados inválidos",
				"A requisição tem campos inválidos. Corrija os campos indicados e tente novamente.");
		problema.setProperty(PROPRIEDADE_ERROS, erros);
		return ResponseEntity.status(status).headers(headers).body(problema);
	}

	private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
		problema.setTitle(titulo);
		return problema;
	}

	/** Um campo inválido da requisição e a mensagem de validação correspondente. */
	public record CampoInvalido(String campo, String mensagem) {
	}

}
