package br.org.cesjo.sgi.listaespera.interno;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Capacidade total do serviço (ADR 0092). */
record NovaCapacidade(@NotNull @Min(0) @Max(10000) Integer vagas) {
}
