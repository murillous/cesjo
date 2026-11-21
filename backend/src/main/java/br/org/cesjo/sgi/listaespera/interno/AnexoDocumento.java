package br.org.cesjo.sgi.listaespera.interno;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Arquivo já enviado por {@code POST /api/arquivos} para anexar como documento da inscrição. */
record AnexoDocumento(@NotNull UUID arquivoId) {
}
