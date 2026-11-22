package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.atividades.TipoAtividade;

/**
 * Campos da atividade (RF010, ADR 0150), no registro e na correção.
 *
 * @param participantes {@code pessoa_id} dos usuários atendidos que participaram
 * @param profissionais {@code id} dos colaboradores que conduziram a atividade
 */
// PENDENTE-CESJO(P23): participantes são sempre usuários atendidos ativos (pessoa_id); nas atividades com
// famílias, o participante indica de qual usuário é a família, sem cadastrar o familiar (ADR 0150).
record DadosAtividade(@NotNull TipoAtividade tipo,
		@NotNull @PastOrPresent(message = "a data da atividade não pode ser futura") LocalDate data,
		@NotBlank @Size(max = 200) String titulo, @NotBlank @Size(max = 10_000) String descricao,
		@NotEmpty(message = "escolha pelo menos um participante") @Size(max = 200) List<@NotNull UUID> participantes,
		@NotEmpty(message = "escolha pelo menos um profissional") @Size(max = 50) List<@NotNull UUID> profissionais) {
}
