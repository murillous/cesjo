import { Routes } from '@angular/router';

/** Item exibido no menu lateral. `icone` é uma classe do PrimeIcons, por exemplo `pi pi-home`. */
export interface ItemMenu {
  rotulo: string;
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
}
