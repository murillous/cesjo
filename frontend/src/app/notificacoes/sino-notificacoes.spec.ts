import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { Permissao } from '../acesso/permissao';
import { ContaSessao, Sessao } from '../acesso/sessao';
import { Notificacao, PaginaNotificacoes } from './notificacoes-api';
import { INTERVALO_CONSULTA_NOTIFICACOES, SinoNotificacoes } from './sino-notificacoes';

const CONTAGEM = '/api/notificacoes/nao-lidas/contagem';

function conta(permissoes: Permissao[]): ContaSessao {
  return {
    id: 'conta-1',
    login: 'teste',
    nome: 'Conta de teste',
    perfil: 'EQUIPE_TECNICA',
    permissoes,
    trocaSenhaObrigatoria: false,
  };
}

function notificacao(id: string, extra: Partial<Notificacao> = {}): Notificacao {
  return {
    id,
    tipo: 'VAGA_ABERTA',
    mensagem: `Mensagem ${id}`,
    referencia: null,
    criadaEm: '2026-11-20T12:00:00Z',
    lidaEm: null,
    lida: false,
    ...extra,
  };
}

function pagina(itens: Notificacao[], totalPages = 1, number = 0): PaginaNotificacoes {
  return { content: itens, page: { size: 20, number, totalElements: itens.length, totalPages } };
}

describe('SinoNotificacoes', () => {
  const http = () => TestBed.inject(HttpTestingController);
  const sessao = () => TestBed.inject(Sessao);

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SinoNotificacoes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
        { provide: INTERVALO_CONSULTA_NOTIFICACOES, useValue: 60_000 },
      ],
    });
  });

  afterEach(() => {
    document.querySelector('.cdk-overlay-container')?.replaceChildren();
  });

  async function renderizar() {
    const fixture = TestBed.createComponent(SinoNotificacoes);
    await fixture.whenStable();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  async function responderContagem(
    fixture: { whenStable(): Promise<unknown> },
    quantidade: number,
  ) {
    http().expectOne(CONTAGEM).flush({ quantidade });
    await fixture.whenStable();
  }

  function painel(): HTMLElement | null {
    return document.querySelector('#painel-notificacoes');
  }

  it('não aparece nem consulta o backend sem sessão', async () => {
    const { elemento } = await renderizar();

    expect(elemento.querySelector('button.sino')).toBeNull();
    http().expectNone(CONTAGEM);
  });

  it('não aparece para conta sem NOTIFICACOES_LER', async () => {
    sessao().definir(conta([]));
    const { elemento } = await renderizar();

    expect(elemento.querySelector('button.sino')).toBeNull();
    http().expectNone(CONTAGEM);
  });

  it('mostra o contador de não lidas e anuncia para leitor de tela', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 3);

    const botao = elemento.querySelector<HTMLButtonElement>('button.sino');
    expect(botao?.getAttribute('aria-label')).toBe('Notificações, 3 não lidas');
    expect(elemento.querySelector('.contador')?.textContent?.trim()).toBe('3');
    expect(elemento.querySelector('[aria-live="polite"]')?.textContent?.trim()).toBe(
      '3 notificações não lidas',
    );
  });

  it('limita o número exibido a 99+', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 150);

    expect(elemento.querySelector('.contador')?.textContent?.trim()).toBe('99+');
  });

  it('sem não lidas esconde o contador', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 0);

    expect(elemento.querySelector('.contador')).toBeNull();
    expect(elemento.querySelector('button.sino')?.getAttribute('aria-label')).toBe(
      'Notificações, nenhuma não lida',
    );
  });

  it('atualiza o contador pela consulta periódica', async () => {
    // Só o setInterval (usado pelo `interval` do RxJS) é falso: o agendador do Angular segue com o
    // relógio real, e o intervalo avança só quando o teste manda.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    try {
      sessao().definir(conta(['NOTIFICACOES_LER']));
      const { fixture, elemento } = await renderizar();
      await responderContagem(fixture, 1);
      http().expectNone(CONTAGEM);

      vi.advanceTimersByTime(59_999);
      http().expectNone(CONTAGEM);

      vi.advanceTimersByTime(1);
      await responderContagem(fixture, 2);
      expect(elemento.querySelector('.contador')?.textContent?.trim()).toBe('2');

      vi.advanceTimersByTime(60_000);
      await responderContagem(fixture, 4);
      expect(elemento.querySelector('.contador')?.textContent?.trim()).toBe('4');

      fixture.destroy();
      vi.advanceTimersByTime(60_000);
      http().expectNone(CONTAGEM);
    } finally {
      vi.useRealTimers();
    }
  });

  it('abre o painel com a lista e marca uma como lida', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 2);

    elemento.querySelector<HTMLButtonElement>('button.sino')?.click();
    await fixture.whenStable();
    http()
      .expectOne((req) => req.url === '/api/notificacoes' && req.params.get('page') === '0')
      .flush(
        pagina([
          notificacao('a'),
          notificacao('b', { lida: true, lidaEm: '2026-11-20T13:00:00Z' }),
        ]),
      );
    await responderContagem(fixture, 1);

    expect(painel()?.getAttribute('role')).toBe('dialog');
    const itens = painel()?.querySelectorAll('li.item') ?? [];
    expect(itens.length).toBe(2);
    expect(itens[0].classList).toContain('nao-lida');
    expect(itens[1].querySelector('button.marcar')).toBeNull();

    itens[0].querySelector<HTMLButtonElement>('button.marcar')?.click();
    const marcar = http().expectOne('/api/notificacoes/a/leitura');
    expect(marcar.request.method).toBe('POST');
    marcar.flush(null);
    await fixture.whenStable();

    expect(painel()?.querySelector('li.item')?.classList).not.toContain('nao-lida');
    expect(elemento.querySelector('.contador')).toBeNull();
  });

  it('marca todas como lidas', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 2);
    elemento.querySelector<HTMLButtonElement>('button.sino')?.click();
    await fixture.whenStable();
    http()
      .expectOne((req) => req.url === '/api/notificacoes')
      .flush(pagina([notificacao('a'), notificacao('b')]));
    await responderContagem(fixture, 2);

    painel()?.querySelector<HTMLButtonElement>('button.marcar-todas')?.click();
    const todas = http().expectOne('/api/notificacoes/leitura');
    expect(todas.request.method).toBe('POST');
    todas.flush({ marcadas: 2 });
    await fixture.whenStable();

    expect(painel()?.querySelectorAll('li.nao-lida').length).toBe(0);
    expect(elemento.querySelector('.contador')).toBeNull();
  });

  it('mostra aviso de lista vazia e de erro', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 0);
    const botao = elemento.querySelector<HTMLButtonElement>('button.sino');

    botao?.click();
    await fixture.whenStable();
    http()
      .expectOne((req) => req.url === '/api/notificacoes')
      .flush(pagina([]));
    await responderContagem(fixture, 0);
    expect(painel()?.textContent).toContain('Nenhuma notificação por enquanto.');

    botao?.click();
    await fixture.whenStable();
    botao?.click();
    await fixture.whenStable();
    http()
      .expectOne((req) => req.url === '/api/notificacoes')
      .flush(null, { status: 500, statusText: 'Erro' });
    await responderContagem(fixture, 0);
    expect(painel()?.querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível carregar as notificações.',
    );
  });

  it('abre a tela da referência ao clicar na mensagem', async () => {
    sessao().definir(conta(['NOTIFICACOES_LER']));
    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const { fixture, elemento } = await renderizar();
    await responderContagem(fixture, 1);
    elemento.querySelector<HTMLButtonElement>('button.sino')?.click();
    await fixture.whenStable();
    http()
      .expectOne((req) => req.url === '/api/notificacoes')
      .flush(pagina([notificacao('a', { referencia: '/lista-espera' })]));
    await responderContagem(fixture, 1);

    painel()?.querySelector<HTMLAnchorElement>('a.mensagem')?.click();
    http().expectOne('/api/notificacoes/a/leitura').flush(null);
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith('/lista-espera');
    expect(painel()).toBeNull();
  });
});
