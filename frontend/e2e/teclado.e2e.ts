import { expect, Page, test } from '@playwright/test';

import { simularApi } from './api-simulada';

/**
 * Navegação por teclado nos formulários (issue #24, ADR 0123): cada campo é alcançado com Tab, o
 * foco fica visível e nunca escondido atrás das barras presas (WCAG 2.1.1, 2.4.7 e 2.4.11).
 */

/** Confere que o elemento focado não está atrás da barra superior nem da navegação inferior. */
async function focoVisivel(page: Page): Promise<string> {
  return page.evaluate(() => {
    const foco = document.activeElement as HTMLElement | null;
    if (!foco || foco === document.body) return 'nenhum elemento focado';
    const caixa = foco.getBoundingClientRect();
    const topo = document.querySelector('.topbar')?.getBoundingClientRect().bottom ?? 0;
    const navegacao = document.querySelector('.navegacao')?.getBoundingClientRect();
    // No celular a navegação é uma barra na base; no tablet e no desktop, um trilho lateral.
    const base =
      navegacao && navegacao.width < window.innerWidth / 2
        ? window.innerHeight
        : (navegacao?.top ?? window.innerHeight);
    const nome =
      foco.getAttribute('formcontrolname') ?? foco.getAttribute('aria-label') ?? foco.tagName;
    if (caixa.top < topo - 1 || caixa.bottom > base + 1)
      return `${nome} escondido atrás de uma barra (${Math.round(caixa.top)}..${Math.round(caixa.bottom)}, livre ${Math.round(topo)}..${Math.round(base)}, sp ${getComputedStyle(document.documentElement).scrollPaddingBottom}, y ${Math.round(window.scrollY)}/${document.documentElement.scrollHeight - window.innerHeight})`;
    const estilo = getComputedStyle(foco);
    const borda = (el: Element, pseudo?: string) => {
      const e = getComputedStyle(el, pseudo);
      return (
        (e.outlineStyle !== 'none' && parseFloat(e.outlineWidth) > 0) ||
        (e.borderTopStyle !== 'none' && parseFloat(e.borderTopWidth) > 0)
      );
    };
    // Outline nativo ou o indicador forte do Material (borda no ::before de .mat-focus-indicator).
    const indicadores = [
      foco,
      ...foco.querySelectorAll('.mat-focus-indicator, .mat-mdc-focus-indicator'),
    ];
    const contorno =
      (estilo.outlineStyle !== 'none' && parseFloat(estilo.outlineWidth) > 0) ||
      indicadores.some((el) => borda(el, '::before'));
    // Os campos do Material mostram o foco na linha ativa e no rótulo, não no outline do input.
    const campoMaterial = foco.closest('.mat-mdc-form-field.mat-focused') !== null;
    if (!contorno && !campoMaterial && estilo.boxShadow === 'none')
      return `${nome} sem indicador de foco`;
    return 'ok';
  });
}

// O Safari só leva o Tab a campos de texto, a menos que a pessoa ligue "Pressionar Tab para
// destacar cada item"; o percurso com Tab fica com Chromium e Firefox e o Safari, com a checklist.
test.skip(
  ({ browserName }) => browserName === 'webkit',
  'Tab do Safari depende de ajuste do sistema',
);

test('entrar só com o teclado', async ({ page }) => {
  await simularApi(page, { logado: false });
  await page.goto('/entrar');
  await page.getByLabel('Login').focus();
  await page.keyboard.type('coordenacao');
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Senha', { exact: true })).toBeFocused();
  await page.keyboard.type('senha-ficticia-de-teste');
  await page.keyboard.press('Enter');
  await expect(page).toHaveTitle(/Início/);
});

test('cadastro de candidato: Tab percorre todos os campos com o foco visível', async ({ page }) => {
  await simularApi(page);
  await page.goto('/lista-espera/nova');
  await expect(page).toHaveTitle(/Cadastrar candidato/);

  const campos = page.locator('form [formcontrolname]');
  const total = await campos.count();
  expect(total).toBeGreaterThan(0);

  await campos.first().focus();
  const alcancados = new Set<string>();
  for (let passo = 0; passo < total * 4 && alcancados.size < total; passo++) {
    const nome = await page.evaluate(
      () =>
        document.activeElement?.closest('[formcontrolname]')?.getAttribute('formcontrolname') ??
        null,
    );
    if (nome) {
      alcancados.add(nome);
      // Espera a rolagem do layout, feita no quadro seguinte ao foco.
      await expect.poll(() => focoVisivel(page), { message: `foco em ${nome}` }).toBe('ok');
    }
    await page.keyboard.press('Tab');
  }
  const nomes = await campos.evaluateAll((els) =>
    els.map((e) => e.getAttribute('formcontrolname')),
  );
  expect([...alcancados].sort()).toEqual([...new Set(nomes)].sort());

  // O botão de envio também é alcançável e dispara a validação pelo teclado.
  const enviar = page.getByRole('button', { name: 'Cadastrar candidato' });
  for (
    let passo = 0;
    passo < 10 && !(await enviar.evaluate((b) => b === document.activeElement));
    passo++
  ) {
    await page.keyboard.press('Tab');
  }
  await expect(enviar).toBeFocused();
  await expect.poll(() => focoVisivel(page)).toBe('ok');
  await page.keyboard.press('Enter');
  await expect(page.locator('mat-error').first()).toBeVisible();
});
