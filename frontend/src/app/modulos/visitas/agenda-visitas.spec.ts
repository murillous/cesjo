import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { AgendaVisitas } from './agenda-visitas';
import { visitaFicticia } from './visita-ficticia';

describe('AgendaVisitas', () => {
  let controle: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AgendaVisitas],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste());
  });

  afterEach(() => controle.verify());

  it('mostra a semana pedida, de segunda a domingo, com as visitas no dia certo', async () => {
    const fixture = TestBed.createComponent(AgendaVisitas);
    fixture.componentRef.setInput('semana', '2026-11-25');
    await fixture.whenStable();

    const pedido = controle.expectOne((r) => r.url === '/api/visitas');
    expect(pedido.request.params.get('de')).toBe('2026-11-23');
    expect(pedido.request.params.get('ate')).toBe('2026-11-29');
    pedido.flush([
      visitaFicticia({ id: 'v1', dataHora: '2026-11-25T08:00:00', nomePessoa: 'Pessoa Manhã' }),
      visitaFicticia({
        id: 'v2',
        dataHora: '2026-11-25T15:00:00',
        nomePessoa: 'Pessoa Tarde',
        status: 'CANCELADA',
      }),
    ]);
    await fixture.whenStable();

    const dias = (fixture.nativeElement as HTMLElement).querySelectorAll('li.dia');
    expect(dias).toHaveLength(7);
    expect(dias[0].textContent).toContain('23');
    expect(dias[2].textContent).toContain('08:00');
    expect(dias[2].textContent).toContain('Pessoa Manhã');
    expect(dias[2].textContent).toContain('Cancelada');
    expect(dias[3].textContent).toContain('Sem visitas');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Agendar visita');
  });

  it('não oferece agendar a quem só lê', async () => {
    TestBed.inject(Sessao).definir(contaDeTeste({ permissoes: ['VISITAS_LER'] }));
    const fixture = TestBed.createComponent(AgendaVisitas);
    await fixture.whenStable();
    controle.expectOne((r) => r.url === '/api/visitas').flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Agendar visita');
  });

  it('mostra o erro da API', async () => {
    const fixture = TestBed.createComponent(AgendaVisitas);
    await fixture.whenStable();
    controle
      .expectOne((r) => r.url === '/api/visitas')
      .flush(
        { detail: 'Consulte no máximo 62 dias de cada vez.' },
        { status: 422, statusText: 'x' },
      );
    await fixture.whenStable();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent,
    ).toContain('Consulte no máximo 62 dias de cada vez.');
  });
});
