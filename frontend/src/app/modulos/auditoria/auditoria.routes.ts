import { Routes } from '@angular/router';

import { ConsultaAuditoria } from './consulta-auditoria';

export const AUDITORIA_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: ConsultaAuditoria,
    title: 'Log de auditoria | CESJO SGI',
  },
];
