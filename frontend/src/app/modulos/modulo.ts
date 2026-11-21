import { Routes } from '@angular/router';

import { Permissao } from '../acesso/permissao';

/** Item exibido no menu lateral. `icone` é o nome de um ícone Material Symbols, por exemplo `home`. */
export interface ItemMenu {
  rotulo: string;
  /**
   * Versão curta do rótulo para a barra inferior do celular, onde cada item tem um quinto da
   * largura da tela (cabe por volta de 10 caracteres). Sem ela, a barra usa `rotulo`.
   */
  rotuloCurto?: string;
  icone: string;
  /** Posição no menu; menor aparece primeiro. */
  ordem: number;
}

/**
 * Contrato de um módulo do frontend (lista de espera, atendimento etc.).
 *
 * Cada módulo vive em `src/app/modulos/<modulo>/`, declara aqui seu caminho, suas rotas
 * (carregadas sob demanda) e, se quiser, seu item de menu. O registro em `registro.ts`
 * só lista os módulos; rotas e menu são montados a partir dele.
 */
export interface ModuloFrontend {
  /** Segmento de URL do módulo, sem barra inicial. `''` é a página inicial. */
  caminho: string;
  /** Rotas internas do módulo, importadas sob demanda. */
  rotas: () => Promise<Routes>;
  menu?: ItemMenu;
  /** Permissão exigida para entrar no módulo e ver o item de menu. Sem ela, o módulo é de todos. */
  permissao?: Permissao;
}
