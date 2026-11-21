package br.org.cesjo.sgi.acesso.interno;

/**
 * Resposta da criação de conta. A senha temporária aparece só nesta resposta; o sistema guarda apenas
 * o hash.
 */
record ContaCriada(ContaResposta conta, String senhaTemporaria) {
}
