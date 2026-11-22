import { EventoInstitucional } from './eventos-api';

/**
 * Evento fictício para os testes do módulo. Fica fora dos arquivos `.spec.ts` porque um spec que
 * importa outro faz o builder de testes perder o `describe` do importado.
 */
export function eventoFicticio(mudancas: Partial<EventoInstitucional> = {}): EventoInstitucional {
  return {
    id: '00000000-0000-0000-0000-0000000000e1',
    tipo: 'REUNIAO_COORDENACAO_EQUIPE',
    direcao: null,
    data: '2026-03-02',
    descricao: 'Pauta do mês.',
    participantes: null,
    parceiro: null,
    criadoEm: '2026-03-02T12:00:00Z',
    atualizadoEm: '2026-03-02T12:00:00Z',
    ...mudancas,
  };
}
