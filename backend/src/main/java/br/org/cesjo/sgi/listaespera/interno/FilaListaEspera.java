package br.org.cesjo.sgi.listaespera.interno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.listaespera.SituacaoInscricao;
import br.org.cesjo.sgi.pessoas.FiltroPessoas;
import br.org.cesjo.sgi.pessoas.Pessoas;
import br.org.cesjo.sgi.pessoas.ResumoPessoa;

/**
 * Listagem da fila com a posição de cada candidato (RF002, ADR 0057 e ADR 0059).
 *
 * <p>
 * A posição sai da ordem de {@link OrdemDaFila} entre todas as inscrições aguardando, sem olhar os
 * filtros: filtrar por origem não muda a posição de ninguém. Nome e tipo de deficiência são do módulo
 * Pessoas e chegam pela busca em lote {@link Pessoas#resumos}; por isso a paginação é feita aqui,
 * depois do filtro, sobre as inscrições já em ordem.
 */
@Service
class FilaListaEspera {

	private final InscricaoRepositorio inscricoes;

	private final Pessoas pessoas;

	FilaListaEspera(InscricaoRepositorio inscricoes, Pessoas pessoas) {
		this.inscricoes = inscricoes;
		this.pessoas = pessoas;
	}

	@Transactional(readOnly = true)
	Page<ItemFila> listar(FiltroFila filtro, Pageable pagina) {
		Map<UUID, Integer> posicoes = posicoes();
		List<InscricaoListaEspera> candidatas = inscricoes.findAll(filtrar(filtro), OrdemDaFila.ORDEM);
		Map<UUID, ResumoPessoa> resumos = pessoas.resumos(
				candidatas.stream().map(InscricaoListaEspera::getPessoaId).toList(),
				new FiltroPessoas(filtro.nome(), filtro.tipoDeficiencia()));
		List<InscricaoListaEspera> encontradas = candidatas.stream()
			.filter(inscricao -> resumos.containsKey(inscricao.getPessoaId()))
			.toList();

		int inicio = (int) Math.min(pagina.getOffset(), encontradas.size());
		int fim = Math.min(inicio + pagina.getPageSize(), encontradas.size());
		List<ItemFila> itens = encontradas.subList(inicio, fim)
			.stream()
			.map(inscricao -> item(inscricao, posicoes.get(inscricao.getId()), resumos.get(inscricao.getPessoaId())))
			.toList();
		return new PageImpl<>(itens, pagina, encontradas.size());
	}

	/** Posição da inscrição na fila, ou nula quando ela não está aguardando. */
	@Transactional(readOnly = true)
	Integer posicao(UUID inscricaoId) {
		return posicoes().get(inscricaoId);
	}

	/** Posição de cada inscrição aguardando, a partir de 1. */
	private Map<UUID, Integer> posicoes() {
		List<InscricaoListaEspera> fila = inscricoes.findBySituacao(SituacaoInscricao.AGUARDANDO, OrdemDaFila.ORDEM);
		Map<UUID, Integer> posicoes = new HashMap<>();
		for (int i = 0; i < fila.size(); i++) {
			posicoes.put(fila.get(i).getId(), i + 1);
		}
		return posicoes;
	}

	private static ItemFila item(InscricaoListaEspera inscricao, Integer posicao, ResumoPessoa pessoa) {
		boolean completo = DocumentosObrigatorios.pendentes(pessoa.documentos()).isEmpty();
		return new ItemFila(inscricao.getId(), posicao, pessoa.nomeCompleto(), inscricao.getSituacao(),
				inscricao.getDataCadastro(), pessoa.tipoDeficiencia(), inscricao.getOrigemEncaminhamento(), completo);
	}

	private static Specification<InscricaoListaEspera> filtrar(FiltroFila filtro) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condicoes = new ArrayList<>();
			if (filtro.situacao() != null) {
				condicoes.add(cb.equal(raiz.get("situacao"), filtro.situacao()));
			}
			if (filtro.origem() != null) {
				condicoes.add(cb.equal(raiz.get("origemEncaminhamento"), filtro.origem()));
			}
			return cb.and(condicoes.toArray(Predicate[]::new));
		};
	}

}
