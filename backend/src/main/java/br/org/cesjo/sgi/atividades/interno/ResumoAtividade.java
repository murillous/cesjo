package br.org.cesjo.sgi.atividades.interno;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import br.org.cesjo.sgi.atividades.TipoAtividade;
import br.org.cesjo.sgi.institucional.ResumoColaborador;

/** Atividade na listagem: sem a descrição, que só sai na ficha. */
record ResumoAtividade(UUID id, TipoAtividade tipo, LocalDate data, String titulo, List<Participante> participantes,
		List<ResumoColaborador> profissionais) {
}
