import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS, provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { FichaVisita } from './ficha-visita';
import { Visita } from './visitas-api';
import { visitaFicticia } from './visita-ficticia';

interface FichaExposta {
  formRealizar: { patchValue(v: object): void };
  formCancelar: { patchValue(v: object): void };
  formReagendar: { patchValue(v: object): void };
  abrir(acao: string | null): void;
  realizar(v: Visita): void;
  cancelar(v: Visita): void;
  reagendar(v: Visita): void;
}

describe('FichaVisita', () => {
  let controle: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FichaVisita],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definir(contaDeTeste());
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  async function abrirFicha(
    visita: Visita,
  ): Promise<{ fixture: ComponentFixture<FichaVisita>; ficha: FichaExposta }> {
    const fixture = TestBed.createComponent(FichaVisita);
    fixture.componentRef.setInput('id', visita.id);
    await fixture.whenStable();
    controle.expectOne(`/api/visitas/${visita.id}`).flush(visita);
    await fixture.whenStable();
    return { fixture, ficha: fixture.componentInstance as unknown as FichaExposta };
  }

  const texto = (fixture: ComponentFixture<FichaVisita>) =>
    (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('registra a realização com observações', async () => {
    const visita = visitaFicticia({ id: 'v1', dataHora: '2026-01-05T09:00:00' });
    const { fixture, ficha } = await abrirFicha(visita);
    ficha.abrir('realizar');
    ficha.formRealizar.patchValue({ observacoes: '  Casa adaptada (fictício). ' });
    ficha.realizar(visita);

    const pedido = controle.expectOne('/api/visitas/v1/realizar');
    expect(pedido.request.body).toEqual({ observacoes: 'Casa adaptada (fictício).' });
    pedido.flush({ ...visita, status: 'REALIZADA', temObservacoes: true });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Realizada');
    expect(texto(fixture)).toContain('Ler observações');
  });

  it('só lê as observações quando a pessoa pede', async () => {
    const visita = visitaFicticia({ id: 'v2', status: 'REALIZADA', temObservacoes: true });
    const { fixture } = await abrirFicha(visita);
    controle.expectNone('/api/visitas/v2/observacoes');

    const botao = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    ).find((b) => b.textContent?.includes('Ler observações'));
    botao!.click();
    controle.expectOne('/api/visitas/v2/observacoes').flush({ observacoes: 'Texto fictício' });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Texto fictício');
  });

  it('cancela com motivo', async () => {
    const visita = visitaFicticia({ id: 'v3' });
    const { fixture, ficha } = await abrirFicha(visita);
    ficha.abrir('cancelar');
    ficha.cancelar(visita);
    controle.expectNone('/api/visitas/v3/cancelar');

    ficha.formCancelar.patchValue({ motivo: 'Família viajou' });
    ficha.cancelar(visita);
    controle
      .expectOne('/api/visitas/v3/cancelar')
      .flush({ ...visita, status: 'CANCELADA', motivoCancelamento: 'Família viajou' });
    await fixture.whenStable();

    expect(texto(fixture)).toContain('Família viajou');
    expect(texto(fixture)).not.toContain('O que aconteceu com a visita?');
  });

  it('reagenda e abre a visita nova', async () => {
    const visita = visitaFicticia({ id: 'v4' });
    const { ficha } = await abrirFicha(visita);
    ficha.abrir('reagendar');
    controle.expectOne('/api/visitas/profissionais').flush([]);
    ficha.formReagendar.patchValue({ data: new Date(2026, 11, 2), motivo: 'Chuva' });
    ficha.reagendar(visita);

    const pedido = controle.expectOne('/api/visitas/v4/reagendar');
    expect(pedido.request.body).toEqual({
      dataHora: '2026-12-02T14:30',
      profissionais: ['c1'],
      motivo: 'Chuva',
    });
    pedido.flush(visitaFicticia({ id: 'v5', visitaOrigemId: 'v4' }));

    expect(navegar).toHaveBeenCalledWith(['/visitas', 'v5']);
  });

  it('administrativo cancela e reagenda, mas não registra realização nem lê observações', async () => {
    const permissoes: Permissao[] = ['VISITAS_LER', 'VISITAS_AGENDAR'];
    TestBed.inject(Sessao).definir(contaDeTeste({ perfil: 'ADMINISTRATIVO', permissoes }));
    const { fixture } = await abrirFicha(visitaFicticia({ id: 'v6' }));

    expect(texto(fixture)).toContain('Cancelar visita');
    expect(texto(fixture)).not.toContain('Registrar realização');
  });

  it('não oferece ações em visita já realizada', async () => {
    const { fixture } = await abrirFicha(visitaFicticia({ id: 'v7', status: 'REALIZADA' }));

    expect(texto(fixture)).not.toContain('O que aconteceu com a visita?');
    expect(texto(fixture)).toContain('Sem observações.');
  });
});
