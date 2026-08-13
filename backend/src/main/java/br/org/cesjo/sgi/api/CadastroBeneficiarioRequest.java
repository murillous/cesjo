package br.org.cesjo.sgi.api;

import br.org.cesjo.sgi.domain.OrigemEncaminhamento;
import br.org.cesjo.sgi.domain.TipoDeficiencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastroBeneficiarioRequest(
        @NotBlank(message = "Nome completo é obrigatório")
        @Size(max = 150, message = "Nome completo deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "Data de nascimento é obrigatória")
        LocalDate dataNascimento,

        @NotBlank(message = "Nome do responsável é obrigatório")
        @Size(max = 150, message = "Nome do responsável deve ter no máximo 150 caracteres")
        String responsavel,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        @NotNull(message = "Tipo de deficiência é obrigatório")
        TipoDeficiencia tipoDeficiencia,

        boolean beneficiarioBpc,
        boolean inscritoCadunico,

        @NotNull(message = "Origem do encaminhamento é obrigatória")
        OrigemEncaminhamento origemEncaminhamento,

        @NotNull(message = "Data de entrada é obrigatória")
        LocalDate dataEntrada
) {
}

