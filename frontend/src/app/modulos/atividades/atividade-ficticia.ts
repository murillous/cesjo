import { Atividade } from './atividades-api';

/** Atividade fictícia para os testes. Nenhum dado real (LGPD). */
export function atividadeFicticia(mudancas: Partial<Atividade> = {}): Atividade {
  return {
    id: 'a1',
    tipo: 'COM_USUARIOS',
    data: '2026-11-20',
    titulo: 'Oficina de música',
    descricao: 'Roda de música (fictício).',
    participantes: [{ pessoaId: 'p1', nomeCompleto: 'Ana Fictícia' }],
    profissionais: [{ id: 'c1', nome: 'Terapeuta Fictícia', funcao: 'TERAPEUTA_OCUPACIONAL' }],
    criadoEm: '2026-11-20T13:00:00Z',
    atualizadoEm: '2026-11-20T13:00:00Z',
    ...mudancas,
  };
}
