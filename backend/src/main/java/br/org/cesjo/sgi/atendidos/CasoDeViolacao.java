package br.org.cesjo.sgi.atendidos;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Um usuário atendido com violações de direitos identificadas num período, para o relatório de
 * intervenção (RF016, ADR 0205). Traz o nome, mas nunca a descrição da violação.
 *
 * @param pessoaId pessoa do usuário atendido
 * @param nomeCompleto nome do usuário atendido
 * @param violacoes violações identificadas no período, da mais antiga para a mais recente
 * @param inserido se a pessoa tinha inserção no serviço até o fim do período
 * @param dataDesligamento dia do desligamento até o fim do período, {@code null} se continuava ativa
 * @param motivoDesligamento motivo do desligamento, {@code null} se continuava ativa
 */
public record CasoDeViolacao(UUID pessoaId, String nomeCompleto, List<Violacao> violacoes, boolean inserido,
		LocalDate dataDesligamento, MotivoDesligamento motivoDesligamento) {

	public CasoDeViolacao {
		Objects.requireNonNull(pessoaId, "pessoaId");
		Objects.requireNonNull(nomeCompleto, "nomeCompleto");
		violacoes = List.copyOf(violacoes);
	}

	/**
	 * Uma violação do caso, sem a descrição.
	 *
	 * @param ambito intra ou extrafamiliar, {@code null} nas categorias sem âmbito
	 */
	public record Violacao(TipoViolacao tipo, AmbitoViolacao ambito, LocalDate dataIdentificacao) {

		public Violacao {
			Objects.requireNonNull(tipo, "tipo");
			Objects.requireNonNull(dataIdentificacao, "dataIdentificacao");
		}

	}

}
