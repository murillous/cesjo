package br.org.cesjo.sgi.atendimento.interno;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Data em que o PIA foi apresentado à família, à equipe ou aprovado (ADR 0106). */
record DataDoPasso(@NotNull LocalDate data) {
}
