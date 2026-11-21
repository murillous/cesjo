package br.org.cesjo.sgi.notificacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class NovaNotificacaoTests {

	@Test
	void aceitaTipoMensagemEReferenciaValidos() {
		NovaNotificacao notificacao = new NovaNotificacao("VAGA_ABERTA", "  Vaga aberta.  ", " /lista-espera ");

		assertThat(notificacao.mensagem()).isEqualTo("Vaga aberta.");
		assertThat(notificacao.referencia()).isEqualTo("/lista-espera");
	}

	@Test
	void referenciaEmBrancoViraNula() {
		assertThat(new NovaNotificacao("AVISO", "Texto", " ").referencia()).isNull();
	}

	@Test
	void recusaTipoForaDoFormato() {
		assertThatIllegalArgumentException().isThrownBy(() -> NovaNotificacao.de("vaga aberta", "Texto"));
		assertThatIllegalArgumentException().isThrownBy(() -> NovaNotificacao.de("", "Texto"));
	}

	@Test
	void recusaMensagemVaziaOuLongaDemais() {
		assertThatIllegalArgumentException().isThrownBy(() -> NovaNotificacao.de("AVISO", "   "));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> NovaNotificacao.de("AVISO", "x".repeat(NovaNotificacao.TAMANHO_MAXIMO_MENSAGEM + 1)));
	}

	@Test
	void recusaReferenciaExterna() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new NovaNotificacao("AVISO", "Texto", "https://exemplo.invalid"));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new NovaNotificacao("AVISO", "Texto", "//exemplo.invalid/caminho"));
	}

}
