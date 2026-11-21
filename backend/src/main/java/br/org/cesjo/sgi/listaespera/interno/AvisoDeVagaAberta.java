package br.org.cesjo.sgi.listaespera.interno;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.notificacoes.Notificacoes;
import br.org.cesjo.sgi.notificacoes.NovaNotificacao;

/**
 * Notifica a abertura de vaga (RF003, ADR 0095): quando as vagas disponíveis passam de zero para mais
 * de zero, Coordenador e Administrativo recebem uma notificação que leva à ficha do próximo candidato
 * da fila. A mensagem não traz o nome, porque a notificação não é cifrada (ADR 0031).
 */
@Service
class AvisoDeVagaAberta {

	static final String TIPO = "VAGA_ABERTA";

	// PENDENTE-CESJO(P3): quem é avisado e por qual canal; por enquanto só no sistema, para Coordenador e
	// Administrativo (ADR 0095).
	static final Set<Perfil> DESTINO = Set.of(Perfil.COORDENADOR, Perfil.ADMINISTRATIVO);

	private final InscricaoRepositorio inscricoes;

	private final Notificacoes notificacoes;

	AvisoDeVagaAberta(InscricaoRepositorio inscricoes, Notificacoes notificacoes) {
		this.inscricoes = inscricoes;
		this.notificacoes = notificacoes;
	}

	/** Notifica só na passagem de zero para mais de zero. */
	void avisarSeAbriu(long disponiveisAntes, long disponiveisDepois) {
		if (disponiveisAntes > 0 || disponiveisDepois <= 0) {
			return;
		}
		String vagasTexto = disponiveisDepois == 1 ? "1 vaga disponível" : disponiveisDepois + " vagas disponíveis";
		List<InscricaoListaEspera> fila = inscricoes.findBySituacaoIn(SituacaoInscricao.ATIVAS, OrdemDaFila.ORDEM);
		NovaNotificacao notificacao = fila.isEmpty()
				? new NovaNotificacao(TIPO, "Uma vaga foi aberta no serviço (" + vagasTexto + "). A fila está vazia.",
						"/lista-espera")
				: new NovaNotificacao(TIPO,
						"Uma vaga foi aberta no serviço (" + vagasTexto + "). Veja o próximo candidato da fila.",
						"/lista-espera/" + fila.getFirst().getId());
		notificacoes.notificarPerfis(DESTINO, notificacao);
	}

}
