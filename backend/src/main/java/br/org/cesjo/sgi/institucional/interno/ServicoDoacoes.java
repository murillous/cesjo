package br.org.cesjo.sgi.institucional.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.auditoria.Auditoria;
import br.org.cesjo.sgi.auditoria.EventoAuditoria;
import br.org.cesjo.sgi.compartilhado.seguranca.Identidade;
import br.org.cesjo.sgi.compartilhado.seguranca.IdentidadeAtual;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.compartilhado.web.RegraNegocioException;
import br.org.cesjo.sgi.institucional.DirecaoDoacao;
import br.org.cesjo.sgi.institucional.TipoDoacao;

/** Registro e listagem por período das doações recebidas e realizadas (RF015, ADR 0178). */
@Service
class ServicoDoacoes {

	/** Nome da entidade no log de auditoria. */
	static final String ENTIDADE = "doacao";

	private final DoacaoRepositorio doacoes;

	private final Auditoria auditoria;

	private final IdentidadeAtual identidadeAtual;

	private final Clock relogio = Clock.systemUTC();

	ServicoDoacoes(DoacaoRepositorio doacoes, Auditoria auditoria, IdentidadeAtual identidadeAtual) {
		this.doacoes = doacoes;
		this.auditoria = auditoria;
		this.identidadeAtual = identidadeAtual;
	}

	@Transactional(readOnly = true)
	Page<DoacaoResposta> listar(LocalDate de, LocalDate ate, DirecaoDoacao direcao, TipoDoacao tipo,
			Pageable paginacao) {
		if (de != null && ate != null && ate.isBefore(de)) {
			throw new RegraNegocioException("O fim do período não pode ser antes do início.");
		}
		return doacoes.findAll(DoacaoRepositorio.filtro(de, ate, direcao, tipo), paginacao).map(DoacaoResposta::de);
	}

	@Transactional(readOnly = true)
	DoacaoResposta obter(UUID id) {
		return DoacaoResposta.de(buscar(id));
	}

	@Transactional
	DoacaoResposta registrar(DadosDoacao dados) {
		Doacao doacao = doacoes.save(new Doacao(dados, autor(), relogio.instant()));
		auditoria.registrar(EventoAuditoria.criacao(ENTIDADE, doacao.getId()));
		return DoacaoResposta.de(doacao);
	}

	/** Troca os dados e registra na auditoria só os campos que mudaram de fato. */
	@Transactional
	DoacaoResposta editar(UUID id, DadosDoacao dados) {
		Doacao doacao = buscar(id);
		Set<String> alterados = new LinkedHashSet<>();
		marcar(alterados, "direcao", doacao.getDirecao(), dados.direcao());
		marcar(alterados, "data", doacao.getData(), dados.data());
		marcar(alterados, "contraparte", doacao.getContraparte(), dados.contraparte().strip());
		marcar(alterados, "tipo", doacao.getTipo(), dados.tipo());
		marcar(alterados, "descricao", doacao.getDescricao(), dados.descricao().strip());
		marcar(alterados, "quantidade", doacao.getQuantidade(), DadosDoacao.texto(dados.quantidade()));
		marcar(alterados, "valor", doacao.getValor(), dados.valorNormalizado());
		if (!alterados.isEmpty()) {
			doacao.aplicar(dados, autor(), relogio.instant());
			auditoria.registrar(EventoAuditoria.alteracao(ENTIDADE, doacao.getId(), alterados));
		}
		return DoacaoResposta.de(doacao);
	}

	private static void marcar(Set<String> alterados, String campo, Object antes, Object depois) {
		if (!Objects.equals(antes, depois)) {
			alterados.add(campo);
		}
	}

	private Doacao buscar(UUID id) {
		return doacoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada."));
	}

	private UUID autor() {
		return identidadeAtual.obter().map(Identidade::id).orElse(null);
	}

}
