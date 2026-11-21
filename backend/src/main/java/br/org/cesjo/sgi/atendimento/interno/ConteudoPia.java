package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Conteúdo do PIA, guardado como JSON cifrado (ADR 0103).
 *
 * <p>
 * PENDENTE-CESJO(P5): estrutura provisória, enquanto a equipe não manda o modelo do PIA: um diagnóstico
 * em texto livre, objetivos com metas, ações, prazo e responsável, e observações. Mudar aqui não exige
 * migração, porque o banco guarda o JSON inteiro.
 *
 * @param diagnostico situação da pessoa e da família que o plano quer mudar
 * @param objetivos objetivos do plano, na ordem de prioridade
 */
record ConteudoPia(@Size(max = 10_000) String diagnostico,
		@NotNull @Size(max = 30, message = "use no máximo 30 objetivos") List<@NotNull @Valid Objetivo> objetivos,
		@Size(max = 10_000) String observacoes) {

	/**
	 * Um objetivo do PIA.
	 *
	 * @param responsavel quem cuida do objetivo: profissional, família ou outro serviço
	 */
	record Objetivo(@NotBlank(message = "descreva o objetivo") @Size(max = 2_000) String descricao,
			@Size(max = 5_000) String metas, @Size(max = 5_000) String acoes, LocalDate prazo,
			@Size(max = 150) String responsavel) {
	}

}
