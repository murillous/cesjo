package br.org.cesjo.sgi.notificacoes.interno;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.acesso.ContaAtual;
import br.org.cesjo.sgi.acesso.ContaAutenticada;
import br.org.cesjo.sgi.acesso.Perfil;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.notificacoes.Notificacoes;
import br.org.cesjo.sgi.notificacoes.NovaNotificacao;

/**
 * Cria notificações para os outros módulos ({@link Notificacoes}) e cuida da caixa de notificações da
 * conta logada.
 */
@Service
class ServicoNotificacoes implements Notificacoes {

	static final String ENTIDADE = "notificacao";

	static final Set<String> CAMPOS_LEITURA = Set.of("lida_em");

	private final NotificacaoRepositorio notificacoes;

	private final LeituraNotificacaoRepositorio leituras;

	private final ContaAtual contaAtual;

	private final Auditoria auditoria;

	ServicoNotificacoes(NotificacaoRepositorio notificacoes, LeituraNotificacaoRepositorio leituras,
			ContaAtual contaAtual, Auditoria auditoria) {
		this.notificacoes = notificacoes;
		this.leituras = leituras;
		this.contaAtual = contaAtual;
		this.auditoria = auditoria;
	}

	@Override
	@Transactional
	public UUID notificarPerfil(Perfil perfil, NovaNotificacao notificacao) {
		Objects.requireNonNull(perfil, "perfil");
		return criar(Notificacao.paraPerfil(perfil, Objects.requireNonNull(notificacao, "notificacao"), autor()));
	}

	@Override
	@Transactional
	public List<UUID> notificarPerfis(Set<Perfil> perfis, NovaNotificacao notificacao) {
		Objects.requireNonNull(perfis, "perfis");
		return Stream.of(Perfil.values())
			.filter(perfis::contains)
			.map(perfil -> notificarPerfil(perfil, notificacao))
			.toList();
	}

	@Override
	@Transactional
	public UUID notificarConta(UUID contaId, NovaNotificacao notificacao) {
		Objects.requireNonNull(contaId, "contaId");
		return criar(Notificacao.paraConta(contaId, Objects.requireNonNull(notificacao, "notificacao"), autor()));
	}

	@Transactional(readOnly = true)
	Page<NotificacaoResposta> listar(ContaAutenticada conta, boolean apenasNaoLidas, Pageable pagina) {
		// A ordem é sempre da mais nova para a mais antiga; ordenação pedida pelo cliente é ignorada.
		Pageable semOrdenacao = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize());
		return notificacoes.listarDaConta(conta.id(), conta.perfil(), apenasNaoLidas, semOrdenacao);
	}

	@Transactional(readOnly = true)
	long contarNaoLidas(ContaAutenticada conta) {
		return notificacoes.contarNaoLidas(conta.id(), conta.perfil());
	}

	@Transactional
	void marcarComoLida(ContaAutenticada conta, UUID notificacaoId) {
		// Notificação de outra conta ou de outro perfil responde como inexistente, sem revelar que existe.
		if (!notificacoes.visivelParaConta(notificacaoId, conta.id(), conta.perfil())) {
			throw new RecursoNaoEncontradoException("Notificação não encontrada.");
		}
		registrarLeitura(notificacaoId, conta.id(), Instant.now());
	}

	@Transactional
	int marcarTodasComoLidas(ContaAutenticada conta) {
		Instant agora = Instant.now();
		int marcadas = 0;
		for (UUID id : notificacoes.idsNaoLidas(conta.id(), conta.perfil())) {
			if (registrarLeitura(id, conta.id(), agora)) {
				marcadas++;
			}
		}
		return marcadas;
	}

	private boolean registrarLeitura(UUID notificacaoId, UUID contaId, Instant lidaEm) {
		boolean registrada = leituras.registrar(notificacaoId, contaId, lidaEm) == 1;
		if (registrada) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, notificacaoId, CAMPOS_LEITURA));
		}
		return registrada;
	}

	private UUID criar(Notificacao notificacao) {
		notificacoes.save(notificacao);
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, notificacao.id()));
		return notificacao.id();
	}

	private UUID autor() {
		return contaAtual.id().orElse(null);
	}

}
