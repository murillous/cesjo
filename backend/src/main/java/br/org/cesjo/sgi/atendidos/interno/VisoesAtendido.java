package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.atendidos.MotivoDesligamento;
import br.org.cesjo.sgi.atendidos.SituacaoInsercao;
import br.org.cesjo.sgi.atendidos.Turno;
import br.org.cesjo.sgi.listaespera.OrigemEncaminhamento;
import br.org.cesjo.sgi.pessoas.TipoDeficiencia;

/**
 * O que a API mostra de um usuário atendido, conforme a permissão (ADR 0082 e ADR 0083). A visão
 * básica é um tipo à parte, sem os campos da completa, para nenhum dado escapar por engano.
 */
final class VisoesAtendido {

	private VisoesAtendido() {
	}

	// PENDENTE-CESJO(P8): "dados básicos" são só nome, situação, turno e datas (ADR 0015 e ADR 0082).
	/** Item da listagem e ficha para quem só tem {@code ATENDIDOS_LER_BASICO}. */
	record Basico(UUID id, String nomeCompleto, SituacaoInsercao situacao, Turno turno, LocalDate dataInsercao,
			LocalDate dataDesligamento, boolean completo) {

		static Basico de(LinhaAtendido linha) {
			return new Basico(linha.id(), linha.nomeCompleto(), linha.situacao(), linha.turno(), linha.dataInsercao(),
					linha.dataDesligamento(), false);
		}

	}

	/** Item da listagem para quem tem {@code ATENDIDOS_LER}. */
	record Item(UUID id, UUID pessoaId, String nomeCompleto, SituacaoInsercao situacao, Turno turno,
			LocalDate dataInsercao, LocalDate dataDesligamento, TipoDeficiencia tipoDeficiencia, LocalDate dataNascimento, Integer idade,
			FaixaEtaria faixaEtaria, boolean completo) {

		static Item de(LinhaAtendido linha, LocalDate hoje) {
			LocalDate nascimento = linha.dataNascimento();
			return new Item(linha.id(), linha.pessoaId(), linha.nomeCompleto(), linha.situacao(), linha.turno(),
					linha.dataInsercao(), linha.dataDesligamento(), linha.tipoDeficiencia(), nascimento,
					nascimento == null ? null : FaixaEtaria.idade(nascimento, hoje),
					FaixaEtaria.de(nascimento, hoje).orElse(null), true);
		}

	}

	/** Ficha completa: inserção, pessoa, cuidador e inscrição de origem. */
	record Ficha(UUID id, UUID pessoaId, String nomeCompleto, SituacaoInsercao situacao, Turno turno,
			LocalDate dataInsercao, TipoDeficiencia tipoDeficiencia, LocalDate dataNascimento, Integer idade,
			FaixaEtaria faixaEtaria, String telefone, String nomeCuidador, Inscricao inscricao,
			Desligamento desligamento, boolean completo) {
	}

	/** Desligamento na ficha completa. Ler a observação é auditado (ADR 0110). */
	record Desligamento(UUID id, LocalDate data, MotivoDesligamento motivo, String observacao) {
	}

	/** De onde a pessoa veio na lista de espera. */
	record Inscricao(UUID id, LocalDate dataCadastro, OrigemEncaminhamento origemEncaminhamento) {
	}

}
