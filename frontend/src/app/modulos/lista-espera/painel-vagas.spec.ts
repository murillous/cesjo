import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Permissao } from '../../acesso/permissao';
import { Sessao } from '../../acesso/sessao';
import { PainelVagas as Painel } from './lista-espera-api';
import { PainelVagas } from './painel-vagas';

const URL = '/api/lista-espera/vagas';

const PAINEL: Painel = {
  capacidade: 30,
  ocupadas: 28,
  disponiveis: 2,
  tamanhoFila: 14,
  definidaEm: '2026-11-01T12:00:00Z',
};

interface Componente {
  vagas: { setValue(v: number | null): void };
  editar(): void;
  salvar(): void;
}

describe('PainelVagas', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PainelVagas],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizar(painel: Painel = PAINEL, permissoes?: Permissao[]) {
    TestBed.inject(Sessao).definir(contaDeTeste(permissoes ? { permissoes } : {}));
    const fixture = TestBed.createComponent(PainelVagas);
    fixture.detectChanges();
    http.expectOne(URL).flush(painel);
    await fixture.whenStable();
    return {
      fixture,
      elemento: fixture.nativeElement as HTMLElement,
      componente: fixture.componentInstance as unknown as Componente,
    };
  }

  it('mostra vagas disponíveis, fila, capacidade e ocupadas', async () => {
    const { elemento } = await renderizar();
    const texto = elemento.querySelector('.numeros')?.textContent ?? '';
    expect(elemento.querySelector('.principal dd')?.textContent).toContain('2');
    expect(texto).toContain('Vagas disponíveis');
    expect(texto).toContain('14');
    expect(texto).toContain('30');
    expect(texto).toContain('28');
  });

  it('Coordenador altera a capacidade', async () => {
    const { fixture, componente, elemento } = await renderizar();
    componente.editar();
    componente.vagas.setValue(32);
    componente.salvar();

    const pedido = http.expectOne('/api/lista-espera/capacidade');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({ vagas: 32 });
    pedido.flush({ ...PAINEL, capacidade: 32, disponiveis: 4 });
    await fixture.whenStable();
    expect(elemento.querySelector('.principal dd')?.textContent).toContain('4');
  });

  it('não envia capacidade negativa', async () => {
    const { componente } = await renderizar();
    componente.editar();
    componente.vagas.setValue(-1);
    componente.salvar();
    http.expectNone('/api/lista-espera/capacidade');
  });

  it('sem CAPACIDADE_EDITAR, não mostra o botão e avisa quando a capacidade falta', async () => {
    const { elemento } = await renderizar(
      { capacidade: null, ocupadas: 0, disponiveis: 0, tamanhoFila: 3, definidaEm: null },
      ['CAPACIDADE_LER'],
    );
    expect(elemento.querySelector('.alterar')).toBeNull();
    expect(elemento.querySelector('.nota')?.textContent).toContain('O Coordenador define.');
  });
});
