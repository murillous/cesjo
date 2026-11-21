package br.org.cesjo.sgi.acesso.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

class RegraSenhaTests {

	private final RegraSenha regra = new RegraSenha(new PropriedadesAcesso(5, Duration.ofMinutes(15), 12,
			new PropriedadesAcesso.PrimeiraConta("", "", "")));

	@Test
	void aceitaFraseLongaSemRegraDeComposicao() {
		assertThatCode(() -> regra.validar("cavalo correto bateria", "maria")).doesNotThrowAnyException();
	}

	@Test
	void recusaSenhaCurtaLongaDemaisOuIgualAoLogin() {
		assertThatThrownBy(() -> regra.validar("curta", "maria")).isInstanceOf(RegraNegocioException.class)
			.hasMessage("A senha precisa ter pelo menos 12 caracteres.");
		assertThatThrownBy(() -> regra.validar("a".repeat(129), "maria")).isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> regra.validar("            ", "maria")).isInstanceOf(RegraNegocioException.class);
		assertThatThrownBy(() -> regra.validar("Login.Ficticio", "login.ficticio"))
			.isInstanceOf(RegraNegocioException.class)
			.hasMessage("A senha não pode ser igual ao login.");
	}

	@Test
	void geraTemporariasDistintasComOTamanhoCombinado() {
		String primeira = regra.gerarTemporaria();

		assertThat(primeira).hasSize(RegraSenha.TAMANHO_TEMPORARIA).doesNotContainAnyWhitespaces();
		assertThat(regra.gerarTemporaria()).isNotEqualTo(primeira);
	}

}
