package br.org.cesjo.sgi.institucional;

import java.util.UUID;

/** Colaborador como os outros módulos o veem: o bastante para escolher e exibir o profissional. */
public record ResumoColaborador(UUID id, String nome, FuncaoColaborador funcao) {
}
