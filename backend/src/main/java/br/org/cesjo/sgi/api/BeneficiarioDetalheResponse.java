package br.org.cesjo.sgi.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BeneficiarioDetalheResponse(
        Integer id,
        Integer entradaId,
        int posicao,
        String nome,
        LocalDate dataNascimento,
        String responsavel,
        String telefone,
        String tipoDeficiencia,
        boolean beneficiarioBpc,
        boolean inscritoCadunico,
        String origemEncaminhamento,
        LocalDate dataEntrada,
        DocumentoResponse laudoMedico,
        DocumentoResponse comprovanteResidencia,
        LocalDateTime dataCriacao
) {
}
