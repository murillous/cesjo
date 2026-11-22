import { ChangeDetectionStrategy, Component } from '@angular/core';

import { PainelViolacoes } from '../atendidos/painel-violacoes';

/** Violações de direitos de todos os usuários atendidos (`/violacoes`, RF009, ADR 0145). */
@Component({
  selector: 'app-pagina-violacoes',
  imports: [PainelViolacoes],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagina-violacoes.html',
  styleUrl: './pagina-violacoes.scss',
})
export class PaginaViolacoes {}
