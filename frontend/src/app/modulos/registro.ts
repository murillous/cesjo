import { InjectionToken } from '@angular/core';

import { inicioModulo } from './inicio/inicio.modulo';
import { ModuloFrontend } from './modulo';

/**
 * Módulos ativos do frontend. Para adicionar um módulo, crie `modulos/<modulo>/<modulo>.modulo.ts`
 * exportando um `ModuloFrontend` e acrescente-o a esta lista: uma linha por módulo.
 */
export const MODULOS: readonly ModuloFrontend[] = [inicioModulo];

/** Módulos que montam a navegação. Testes substituem por uma lista própria. */
export const MODULOS_FRONTEND = new InjectionToken<readonly ModuloFrontend[]>('MODULOS_FRONTEND', {
  factory: () => MODULOS,
});
