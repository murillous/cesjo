package br.org.cesjo.sgi.atividades;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * API pública dos atendimentos individuais por usuário atendido, para o relatório de intervenção
 * (RF016, ADR 0205). Atendimento individual é o tipo de atividade com {@link TipoAtividade#individual()}:
 * atendimento multiprofissional, atendimento técnico individualizado e estudo de caso.
 *
 * <p>
 * A permissão é de quem chama: este serviço não checa perfil. Só conta e dá a data, sem título, descrição
 * nem profissionais, então não audita.
 */
public interface AtendimentosIndividuais {

	/**
	 * Atendimentos individuais com data entre {@code inicio} e {@code fim}, as duas inclusive, de cada
	 * pessoa de {@code pessoaIds}. Pessoa sem atendimento no período fica fora do mapa.
	 *
	 * @throws IllegalArgumentException quando {@code fim} é anterior a {@code inicio}
	 */
	Map<UUID, Resumo> resumirPorPessoa(Collection<UUID> pessoaIds, LocalDate inicio, LocalDate fim);

	/**
	 * Atendimentos de uma pessoa no período.
	 *
	 * @param quantidade quantos atendimentos individuais
	 * @param ultimo data do mais recente
	 */
	record Resumo(long quantidade, LocalDate ultimo) {

		public Resumo {
			Objects.requireNonNull(ultimo, "ultimo");
		}

	}

}
