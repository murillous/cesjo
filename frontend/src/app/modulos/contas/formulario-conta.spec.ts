import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';

import { contaDeTeste } from '../../acesso/conta-de-teste';
import { Sessao } from '../../acesso/sessao';
import { DialogoSenhaTemporaria } from './dialogo-senha-temporaria';
import { FormularioConta } from './formulario-conta';
import { contaFicticia } from './conta-ficticia';

describe('FormularioConta', () => {
  let controle: HttpTestingController;
  let dialogo: MatDialog;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioConta],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    controle = TestBed.inject(HttpTestingController);
    dialogo = TestBed.inject(MatDialog);
    TestBed.inject(Sessao).definir(contaDeTeste());
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => controle.verify());

  it('cria a conta e mostra a senha temporária', async () => {
    const abrir = vi.spyOn(dialogo, 'open');
    const fixture = TestBed.createComponent(FormularioConta);
    await fixture.whenStable();
    const componente = fixture.componentInstance as unknown as {
      formulario: { setValue(v: object): void };
      salvar(): void;
    };
    componente.formulario.setValue({
      login: 'Nova.Pessoa',
      nome: 'Nova Pessoa',
      perfil: 'CONSULTA',
    });
    componente.salvar();

    const pedido = controle.expectOne('/api/contas');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body).toEqual({
      login: 'Nova.Pessoa',
      nome: 'Nova Pessoa',
      perfil: 'CONSULTA',
    });
    pedido.flush({ conta: contaFicticia(), senhaTemporaria: 'AbcdEfgh23456789' });

    expect(abrir).toHaveBeenCalledWith(
      DialogoSenhaTemporaria,
      expect.objectContaining({
        data: expect.objectContaining({ senha: 'AbcdEfgh23456789' }),
      }),
    );
  });

  it('desativa outra conta depois de confirmar', async () => {
    vi.spyOn(dialogo, 'open').mockReturnValue({ afterClosed: () => of(true) } as never);
    const fixture = TestBed.createComponent(FormularioConta);
    fixture.componentRef.setInput('id', 'a1');
    await fixture.whenStable();
    controle.expectOne('/api/contas/a1').flush(contaFicticia({ id: 'a1' }));
    await fixture.whenStable();

    const botao = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'),
    ).find((b) => b.textContent?.includes('Desativar conta'));
    botao!.click();
    controle.expectOne('/api/contas/a1/desativar').flush(contaFicticia({ id: 'a1', ativa: false }));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reativar conta');
  });

  it('não oferece desativar a própria conta', async () => {
    const propria = contaDeTeste().id;
    const fixture = TestBed.createComponent(FormularioConta);
    fixture.componentRef.setInput('id', propria);
    await fixture.whenStable();
    controle.expectOne(`/api/contas/${propria}`).flush(contaFicticia({ id: propria }));
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Desativar conta');
  });
});
