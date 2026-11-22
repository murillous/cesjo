import { Page, Route } from '@playwright/test';

/** Dados fictícios (LGPD): nenhuma pessoa real. */
export const CONTA_COORDENADOR = {
  id: '00000000-0000-0000-0000-000000000001',
  login: 'coordenacao',
  nome: 'Pessoa Fictícia',
  perfil: 'COORDENADOR',
  permissoes: [
    'CONTAS_GERENCIAR',
    'AUDITORIA_LER',
    'DOCUMENTOS_LER',
    'DOCUMENTOS_ENVIAR',
    'NOTIFICACOES_LER',
    'LISTA_ESPERA_LER',
    'LISTA_ESPERA_CADASTRAR',
    'LISTA_ESPERA_FILA_LER',
    'RELATORIOS_LER',
    'VIOLACOES_LER',
  ],
  trocaSenhaObrigatoria: false,
};

const pagina = <T>(content: T[]) => ({
  content,
  page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 },
});

const CONTAS = [
  {
    ...CONTA_COORDENADOR,
    ativa: true,
    bloqueada: false,
    criadoEm: '2026-11-01T12:00:00Z',
    atualizadoEm: '2026-11-01T12:00:00Z',
  },
  {
    id: '00000000-0000-0000-0000-000000000002',
    login: 'administrativo.ficticio',
    nome: 'Outra Pessoa Fictícia',
    perfil: 'ADMINISTRATIVO',
    ativa: false,
    bloqueada: true,
    trocaSenhaObrigatoria: true,
    criadoEm: '2026-11-02T12:00:00Z',
    atualizadoEm: '2026-11-03T12:00:00Z',
  },
];

const contagens = (pares: [string, string, number][]) =>
  pares.map(([chave, rotulo, total]) => ({ chave, rotulo, total }));

const PAINEL_PERFIL = {
  geradoEm: '2026-11-20T13:00:00Z',
  totalAtivos: 12,
  porTipoDeficiencia: contagens([
    ['FISICA', 'Física', 3],
    ['SENSORIAL', 'Sensorial', 1],
    ['INTELECTUAL', 'Intelectual', 4],
    ['MULTIPLA', 'Múltipla', 1],
    ['TEA', 'TEA', 2],
    ['T21', 'Síndrome de Down (T21)', 1],
  ]),
  porGenero: contagens([
    ['FEMININO', 'Feminino', 5],
    ['MASCULINO', 'Masculino', 6],
    ['OUTRO', 'Outro', 0],
    ['SEM_VALOR', 'Não informado', 1],
  ]),
  porFaixaEtaria: contagens([
    ['JOVEM', 'Jovem (18 a 29 anos)', 7],
    ['ADULTO', 'Adulto (30 a 59 anos)', 5],
  ]),
  porTurno: contagens([
    ['MATUTINO', 'Matutino', 4],
    ['VESPERTINO', 'Vespertino', 5],
    ['INTEGRAL', 'Integral', 3],
  ]),
};

const PAINEL_ATENDIMENTO = {
  geradoEm: '2026-11-20T13:00:00Z',
  inicio: '2026-01-01',
  fim: '2026-11-20',
  violacoesPorCategoria: contagens([
    ['EXPLORACAO_IMAGEM', 'Exploração de imagem', 0],
    ['ISOLAMENTO_SOCIAL', 'Isolamento social', 2],
    ['DISCRIMINACAO_FAMILIAR', 'Atitudes discriminatórias e preconceituosas no seio familiar', 1],
    ['NEGLIGENCIA_ABANDONO', 'Negligência ou abandono', 3],
    ['OUTRAS', 'Outras violações', 0],
  ]),
  atividadesPorTipo: contagens([
    ['COM_FAMILIAS', 'Atividade com famílias', 4],
    ['COM_USUARIOS', 'Atividade com usuários', 18],
    ['ATENDIMENTO_TECNICO_INDIVIDUALIZADO', 'Atendimento técnico individualizado', 9],
    ['ESTUDO_DE_CASO', 'Estudo de caso', 2],
  ]),
  totalAtividades: 33,
  atendidosNoPeriodo: 25,
  totalDesligamentos: 3,
  taxaDesligamento: 0.12,
  desligamentosPorMotivo: [
    { chave: 'EVASAO', rotulo: 'Evasão', total: 1, taxa: 0.04 },
    { chave: 'MUDANCA_MUNICIPIO', rotulo: 'Mudança para outro município', total: 2, taxa: 0.08 },
    { chave: 'OUTROS', rotulo: 'Outros', total: 0, taxa: 0 },
  ],
};

const AUDITORIA = [
  {
    id: '00000000-0000-0000-0000-0000000000a1',
    ocorridoEm: '2026-11-20T13:00:00Z',
    contaId: CONTA_COORDENADOR.id,
    login: 'coordenacao',
    acao: 'LOGIN',
    entidade: 'conta_acesso',
    entidadeId: CONTA_COORDENADOR.id,
    camposAlterados: [],
    ip: '192.0.2.10',
  },
  {
    id: '00000000-0000-0000-0000-0000000000a2',
    ocorridoEm: '2026-11-20T13:05:00Z',
    contaId: CONTA_COORDENADOR.id,
    login: 'coordenacao',
    acao: 'ATUALIZACAO',
    entidade: 'conta_acesso',
    entidadeId: '00000000-0000-0000-0000-000000000002',
    camposAlterados: ['nome', 'perfil'],
    ip: '192.0.2.10',
  },
];

const VIOLACOES = [
  {
    id: '00000000-0000-0000-0000-0000000000c1',
    insercaoId: '00000000-0000-0000-0000-0000000000c2',
    pessoaId: '00000000-0000-0000-0000-0000000000c3',
    nomeCompleto: 'Pessoa Fictícia Atendida',
    tipo: 'VIOLENCIA_FISICA_PSICOLOGICA',
    ambito: 'INTRAFAMILIAR',
    dataIdentificacao: '2026-11-10',
  },
];

const NOTIFICACOES = [
  {
    id: '00000000-0000-0000-0000-0000000000b1',
    tipo: 'INSCRICAO_LISTA_ESPERA',
    mensagem: 'Nova inscrição na lista de espera.',
    referencia: '/lista-espera',
    criadaEm: '2026-11-20T14:00:00Z',
    lidaEm: null,
    lida: false,
  },
];

function json(route: Route, corpo: unknown, status = 200) {
  return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(corpo) });
}

/**
 * Intercepta `/api/**` no navegador. `logado: false` faz `/api/sessao` responder 401, como sem
 * sessão; `trocaSenha: true` simula o primeiro acesso.
 */
export async function simularApi(
  page: Page,
  opcoes: { logado?: boolean; trocaSenha?: boolean } = {},
): Promise<void> {
  const { logado = true, trocaSenha = false } = opcoes;
  await page.route('**/api/**', (route) => {
    const url = new URL(route.request().url());
    const caminho = url.pathname;
    const metodo = route.request().method();

    if (caminho === '/api/sessao') {
      return logado
        ? json(route, { ...CONTA_COORDENADOR, trocaSenhaObrigatoria: trocaSenha })
        : json(route, { title: 'Não autenticado', status: 401 }, 401);
    }
    if (caminho === '/api/sessao/csrf') return route.fulfill({ status: 204 });
    if (caminho === '/api/sessao/entrar') return json(route, CONTA_COORDENADOR);
    if (caminho === '/api/notificacoes/nao-lidas/contagem') return json(route, { quantidade: 1 });
    if (caminho === '/api/notificacoes') return json(route, pagina(NOTIFICACOES));
    if (caminho === '/api/contas' && metodo === 'GET') return json(route, pagina(CONTAS));
    if (caminho.startsWith('/api/contas/')) return json(route, CONTAS[1]);
    if (caminho === '/api/auditoria/entidades') return json(route, ['conta_acesso', 'arquivo']);
    if (caminho === '/api/auditoria') return json(route, pagina(AUDITORIA));
    if (caminho === '/api/estatisticas/dashboards/perfil') return json(route, PAINEL_PERFIL);
    if (caminho === '/api/estatisticas/dashboards/atendimento')
      return json(route, PAINEL_ATENDIMENTO);
    if (caminho === '/api/violacoes') return json(route, pagina(VIOLACOES));
    return json(route, { title: 'Não simulado no teste', status: 404 }, 404);
  });
}
