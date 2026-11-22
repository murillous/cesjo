package br.org.cesjo.sgi.atendidos.interno;

import java.time.LocalDate;
import java.util.UUID;

import br.org.cesjo.sgi.atendidos.AmbitoViolacao;
import br.org.cesjo.sgi.atendidos.TipoViolacao;

/**
 * Uma violação na consulta, sem a descrição (ADR 0145). A descrição só sai pela leitura auditada.
 *
 * @param insercaoId inserção mais recente da pessoa, para abrir a ficha do usuário atendido
 */
record LinhaViolacao(UUID id, UUID insercaoId, UUID pessoaId, String nomeCompleto, TipoViolacao tipo,
		AmbitoViolacao ambito, LocalDate dataIdentificacao) {
}
