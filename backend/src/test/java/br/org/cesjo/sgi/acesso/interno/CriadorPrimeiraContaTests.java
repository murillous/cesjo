package br.org.cesjo.sgi.acesso.interno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.AcaoAuditoria;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Primeira conta de Coordenador por variáveis de ambiente (ADR 0014). */
class CriadorPrimeiraContaTests {

	private final ContaAcessoRepositorio contas = mock(ContaAcessoRepositorio.class);

	private final Auditoria auditoria = mock(Auditoria.class);

	@Test
	@SuppressWarnings("deprecation")
	void criaCoordenadorComTrocaObrigatoriaQuandoNaoHaContas() {
		when(contas.count()).thenReturn(0L);

		criador("Coordenacao", "Coordenação Fictícia", "senha-inicial-longa").run(null);

		ArgumentCaptor<ContaAcesso> salva = ArgumentCaptor.forClass(ContaAcesso.class);
		verify(contas).save(salva.capture());
		assertThat(salva.getValue().getLogin()).isEqualTo("coordenacao");
		assertThat(salva.getValue().getPerfil()).isEqualTo(Perfil.COORDENADOR);
		assertThat(salva.getValue().isTrocaSenhaObrigatoria()).isTrue();
		assertThat(salva.getValue().isAtiva()).isTrue();
		ArgumentCaptor<EventoAuditoria> evento = ArgumentCaptor.forClass(EventoAuditoria.class);
		verify(auditoria).registrar(evento.capture());
		assertThat(evento.getValue().acao()).isEqualTo(AcaoAuditoria.CRIAR);
	}

	@Test
	void naoFazNadaQuandoJaHaContas() {
		when(contas.count()).thenReturn(3L);

		criador("coordenacao", "", "senha-inicial-longa").run(null);

		verify(contas, never()).save(any());
	}

	@Test
	void naoFazNadaSemAsVariaveis() {
		when(contas.count()).thenReturn(0L);

		criador("", "", "").run(null);

		verify(contas, never()).save(any());
	}

	@Test
	void recusaSenhaFracaNaSubida() {
		when(contas.count()).thenReturn(0L);

		assertThatThrownBy(() -> criador("coordenacao", "", "curta").run(null))
			.isInstanceOf(RegraNegocioException.class);
		verify(contas, never()).save(any());
	}

	@SuppressWarnings("deprecation")
	private CriadorPrimeiraConta criador(String login, String nome, String senha) {
		PropriedadesAcesso propriedades = new PropriedadesAcesso(5, Duration.ofMinutes(15), 12,
				new PropriedadesAcesso.PrimeiraConta(login, nome, senha));
		return new CriadorPrimeiraConta(contas, NoOpPasswordEncoder.getInstance(), new RegraSenha(propriedades),
				auditoria, propriedades);
	}

}
