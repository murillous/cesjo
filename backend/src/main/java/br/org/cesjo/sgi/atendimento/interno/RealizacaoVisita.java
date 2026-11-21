package br.org.cesjo.sgi.atendimento.interno;

import jakarta.validation.constraints.Size;

/** Registro da realização da visita (ADR 0073). @param observacoes opcionais, cifradas ao gravar */
record RealizacaoVisita(@Size(max = 5000) String observacoes) {
}
