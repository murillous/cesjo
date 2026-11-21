/** Item de navegação já resolvido: rótulo, ícone Material Symbols e link absoluto. */
export interface EntradaMenu {
  rotulo: string;
  rotuloCurto: string;
  /**
   * Nome acessível do link. Começa pelo rótulo curto e inclui o completo, para conter o texto visível
   * tanto na barra inferior quanto no trilho (WCAG 2.5.3, ADR 0124).
   */
  nomeAcessivel: string;
  icone: string;
  link: string;
}
