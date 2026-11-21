package br.org.cesjo.sgi.notificacoes;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Conteúdo de uma notificação a criar pela API {@link Notificacoes} (ADR 0031).
 *
 * <p>
 * A mensagem é vista por todas as contas do destino, não é cifrada e não expira, então nunca leva dado
 * pessoal: nem nome de pessoa, nem saúde, crença, violação de direitos ou observações (ADR 0031).
 * Prefira mensagens como "Uma vaga foi aberta no serviço." e deixe o detalhe para a tela da
 * referência, que checa a permissão de quem abre.
 *
 * @param tipo código do tipo, em maiúsculas com sublinhado, por exemplo {@code VAGA_ABERTA}; cada
 *        módulo define as suas constantes
 * @param mensagem texto exibido, até {@value #TAMANHO_MAXIMO_MENSAGEM} caracteres
 * @param referencia caminho da tela relacionada no frontend, começando por {@code /}, ou nulo
 */
public record NovaNotificacao(String tipo, String mensagem, String referencia) {

	public static final int TAMANHO_MAXIMO_MENSAGEM = 500;

	public static final int TAMANHO_MAXIMO_REFERENCIA = 300;

	private static final Pattern FORMATO_TIPO = Pattern.compile("[A-Z][A-Z0-9_]{0,59}");

	public NovaNotificacao {
		Objects.requireNonNull(tipo, "tipo");
		Objects.requireNonNull(mensagem, "mensagem");
		if (!FORMATO_TIPO.matcher(tipo).matches()) {
			throw new IllegalArgumentException("Tipo de notificação fora do formato MAIUSCULAS_COM_SUBLINHADO: " + tipo);
		}
		mensagem = mensagem.strip();
		if (mensagem.isEmpty() || mensagem.length() > TAMANHO_MAXIMO_MENSAGEM) {
			throw new IllegalArgumentException(
					"A mensagem da notificação precisa ter de 1 a " + TAMANHO_MAXIMO_MENSAGEM + " caracteres");
		}
		if (referencia != null) {
			referencia = referencia.strip();
			if (referencia.isEmpty()) {
				referencia = null;
			}
			else if (!referencia.startsWith("/") || referencia.startsWith("//")
					|| referencia.length() > TAMANHO_MAXIMO_REFERENCIA) {
				throw new IllegalArgumentException(
						"A referência da notificação precisa ser um caminho interno começando por / com até "
								+ TAMANHO_MAXIMO_REFERENCIA + " caracteres");
			}
		}
	}

	/** Notificação sem tela relacionada. */
	public static NovaNotificacao de(String tipo, String mensagem) {
		return new NovaNotificacao(tipo, mensagem, null);
	}

}
