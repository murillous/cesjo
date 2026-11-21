package br.org.cesjo.sgi.acesso.interno;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades {@code sgi.acesso.*} do módulo de acesso. Os valores padrão ficam no
 * {@code application.properties}, com o motivo nos ADRs 0011 e 0012.
 *
 * @param tentativasMaximas tentativas de login erradas seguidas que bloqueiam a conta
 * @param duracaoBloqueio por quanto tempo a conta fica bloqueada
 * @param senhaTamanhoMinimo tamanho mínimo de senha escolhida pela conta
 * @param primeiraConta Coordenador criado na subida quando não existe nenhuma conta
 */
@Validated
@ConfigurationProperties("sgi.acesso")
record PropriedadesAcesso(@Min(1) int tentativasMaximas, @NotNull Duration duracaoBloqueio,
		@Min(8) int senhaTamanhoMinimo, @Valid @NotNull PrimeiraConta primeiraConta) {

	/**
	 * Dados da primeira conta de Coordenador, vindos de variáveis de ambiente. Vazios quando não
	 * informados.
	 */
	record PrimeiraConta(String login, String nome, String senha) {

		boolean informada() {
			return login != null && !login.isBlank() && senha != null && !senha.isBlank();
		}

	}

}
