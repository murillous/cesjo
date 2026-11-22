package br.org.cesjo.sgi.atividades.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atividades.Comunicacoes;
import br.org.cesjo.sgi.atividades.TipoComunicacao;
import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;

/** Registro, correção, listagem e contagem dos contatos e comunicações (RF011, ADR 0160 e ADR 0161). */
@Service
class ServicoComunicacoes implements Comunicacoes {

	/** "Hoje" é o dia em São Paulo, mesmo com o servidor em UTC. */
	private final Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));

	private final ComunicacaoRepositorio comunicacoes;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	ServicoComunicacoes(ComunicacaoRepositorio comunicacoes, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.comunicacoes = comunicacoes;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional
	ComunicacaoResposta registrar(DadosComunicacao dados) {
		exigirDataValida(dados.data());
		Comunicacao comunicacao = comunicacoes.save(new Comunicacao(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, comunicacao.getId()));
		return resposta(comunicacao);
	}

	@Transactional
	ComunicacaoResposta corrigir(UUID id, DadosComunicacao dados) {
		Comunicacao comunicacao = buscar(id);
		exigirDataValida(dados.data());
		Set<String> alterados = comunicacao.corrigir(dados, autor(), relogio.instant());
		if (!alterados.isEmpty()) {
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, id, alterados));
		}
		return resposta(comunicacao);
	}

	@Transactional(readOnly = true)
	ComunicacaoResposta ler(UUID id) {
		return resposta(buscar(id));
	}

	/** Comunicações que atendem ao filtro, sem a descrição. */
	@Transactional(readOnly = true)
	Page<ResumoComunicacao> listar(FiltroComunicacoes filtro, Pageable pagina) {
		if (filtro.inicio() != null && filtro.fim() != null && filtro.fim().isBefore(filtro.inicio())) {
			throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
		}
		return comunicacoes.findAll(filtro.especificacao(), pagina)
			.map(c -> new ResumoComunicacao(c.getId(), c.getTipo(), c.getData(), c.getContraparte(), c.getAssunto()));
	}

	@Override
	@Transactional(readOnly = true)
	public Map<TipoComunicacao, Long> contarPorTipo(LocalDate inicio, LocalDate fim) {
		if (fim.isBefore(inicio)) {
			throw new IllegalArgumentException("O fim do período não pode ser anterior ao início.");
		}
		Map<TipoComunicacao, Long> contagem = new EnumMap<>(TipoComunicacao.class);
		for (TipoComunicacao tipo : TipoComunicacao.values()) {
			contagem.put(tipo, 0L);
		}
		for (Object[] linha : comunicacoes.contarPorTipo(inicio, fim)) {
			contagem.put((TipoComunicacao) linha[0], (Long) linha[1]);
		}
		return contagem;
	}

	// PENDENTE-CESJO(P30): só se registra contato ou comunicação que já aconteceu; a data não pode ser futura
	// (ADR 0160).
	private void exigirDataValida(LocalDate data) {
		if (data.isAfter(LocalDate.now(relogio))) {
			throw new RegraNegocioException("A data não pode ser futura.");
		}
	}

	private static ComunicacaoResposta resposta(Comunicacao c) {
		return new ComunicacaoResposta(c.getId(), c.getTipo(), c.getData(), c.getContraparte(), c.getAssunto(),
				c.getDescricao(), c.getCriadoEm(), c.getAtualizadoEm());
	}

	private Comunicacao buscar(UUID id) {
		return comunicacoes.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Contato ou comunicação não encontrado."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
