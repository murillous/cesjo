import { InjectionToken } from '@angular/core';

import { atendidosModulo } from './atendidos/atendidos.modulo';
import { atendimentoModulo } from './atendimento/atendimento.modulo';
import { atividadesModulo } from './atividades/atividades.modulo';
import { auditoriaModulo } from './auditoria/auditoria.modulo';
import { comunicacoesModulo } from './comunicacoes/comunicacoes.modulo';
import { colaboradoresModulo } from './colaboradores/colaboradores.modulo';
import { contasModulo } from './contas/contas.modulo';
import { inicioModulo } from './inicio/inicio.modulo';
import { listaEsperaModulo } from './lista-espera/lista-espera.modulo';
import { relatoriosModulo } from './relatorios/relatorios.modulo';
import { violacoesModulo } from './violacoes/violacoes.modulo';
import { visitasModulo } from './visitas/visitas.modulo';
import { ModuloFrontend } from './modulo';

/**
 * Módulos ativos do frontend. Para adicionar um módulo, crie `modulos/<modulo>/<modulo>.modulo.ts`
 * exportando um `ModuloFrontend` e acrescente-o a esta lista: uma linha por módulo.
 */
export const MODULOS: readonly ModuloFrontend[] = [
  inicioModulo,
  listaEsperaModulo,
  atendidosModulo,
  violacoesModulo,
  visitasModulo,
  atendimentoModulo,
  relatoriosModulo,
  atividadesModulo,
  comunicacoesModulo,
  colaboradoresModulo,
  contasModulo,
  auditoriaModulo,
];

/** Módulos que montam a navegação. Testes substituem por uma lista própria. */
export const MODULOS_FRONTEND = new InjectionToken<readonly ModuloFrontend[]>('MODULOS_FRONTEND', {
  factory: () => MODULOS,
});
