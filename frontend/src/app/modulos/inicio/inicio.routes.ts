import { Routes } from '@angular/router';

import { PaginaInicial } from './pagina-inicial';

export const INICIO_ROUTES: Routes = [
  { path: '', pathMatch: 'full', component: PaginaInicial, title: 'Início | CESJO SGI' },
];
