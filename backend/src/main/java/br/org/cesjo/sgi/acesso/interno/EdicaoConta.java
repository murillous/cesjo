package br.org.cesjo.sgi.acesso.interno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.org.cesjo.sgi.acesso.Perfil;

/** Campos que o Coordenador pode alterar numa conta. O login não muda. */
record EdicaoConta(@NotBlank @Size(max = 150) String nome, @NotNull Perfil perfil) {
}
