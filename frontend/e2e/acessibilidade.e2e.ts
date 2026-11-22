import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test } from '@playwright/test';

import { simularApi } from './api-simulada';

/**
 * Auditoria WCAG 2.2 AA com axe nas telas principais (issue #24, ADR 0123). Cada tela roda nos dois
 * projetos do playwright.config.ts: celular e desktop.
 */
const REGRAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

async function auditar(page: Page): Promise<void> {
  // Sem a fonte dos ícones carregada, a ligadura aparece como palavra ("visibility") e o axe a trata
  // como texto visível do botão.
  await page.evaluate(() => document.fonts.ready);
  // As mensagens de erro do Material entram com fade: no meio da transição o axe mede a cor ainda
  // clara e acusa contraste baixo que a tela parada não tem.
  await page.waitForFunction(() =>
    document.getAnimations().every((animacao) => animacao.playState !== 'running'),
  );
  const resultado = await new AxeBuilder({ page }).withTags(REGRAS).analyze();
  // No celular, o axe acusa target-size em todo campo que, na posição de rolagem atual, passa por
  // baixo da navegação inferior presa. Isso não é falta de tamanho: o campo rola para fora dela e,
  // ao receber foco, o scroll-padding o mantém livre (teclado.e2e.ts confere). Só esse motivo é
  // descartado; tamanho ou espaçamento insuficientes de verdade continuam falhando (ADR 0123).
  const violacoes = resultado.violations
    .map((v) =>
      v.id === 'target-size'
        ? { ...v, nodes: v.nodes.filter((n) => !/partially obscured/.test(n.failureSummary ?? '')) }
        : v,
    )
    .filter((v) => v.nodes.length > 0);
  const resumo = violacoes.map((v) => ({
    regra: v.id,
    impacto: v.impact,
    ajuda: v.help,
    alvos: v.nodes.map((n) => `${n.target.join(' ')}: ${n.failureSummary ?? ''}`),
  }));
  expect(resumo, 'violações de acessibilidade').toEqual([]);
}

const TELAS_LOGADAS = [
  { nome: 'início', caminho: '/', titulo: /Início/ },
  { nome: 'lista de espera', caminho: '/lista-espera', titulo: /Lista de espera/ },
  { nome: 'cadastro de candidato', caminho: '/lista-espera/nova', titulo: /Cadastrar candidato/ },
  { nome: 'contas de acesso', caminho: '/contas', titulo: /Contas de acesso/ },
  { nome: 'nova conta', caminho: '/contas/nova', titulo: /Nova conta/ },
  { nome: 'log de auditoria', caminho: '/auditoria', titulo: /Log de auditoria/ },
  { nome: 'violações de direitos', caminho: '/violacoes', titulo: /Violações de direitos/ },
  { nome: 'página não encontrada', caminho: '/nao-existe', titulo: /não encontrada/ },
];

test.describe('acessibilidade', () => {
  test('entrar', async ({ page }) => {
    await simularApi(page, { logado: false });
    await page.goto('/entrar');
    await expect(page).toHaveTitle(/Entrar/);
    await auditar(page);
  });

  test('entrar com erros de validação', async ({ page }) => {
    await simularApi(page, { logado: false });
    await page.goto('/entrar');
    await page.getByRole('button', { name: /entrar/i }).click();
    await auditar(page);
  });

  test('trocar senha', async ({ page }) => {
    await simularApi(page, { trocaSenha: true });
    await page.goto('/trocar-senha');
    await expect(page).toHaveTitle(/Trocar senha/);
    await auditar(page);
  });

  for (const tela of TELAS_LOGADAS) {
    test(tela.nome, async ({ page }) => {
      await simularApi(page);
      await page.goto(tela.caminho);
      await expect(page).toHaveTitle(tela.titulo);
      await page.waitForLoadState('networkidle');
      await auditar(page);
    });
  }

  test('cadastro de candidato com erros de validação', async ({ page }) => {
    await simularApi(page);
    await page.goto('/lista-espera/nova');
    await page.getByRole('button', { name: /cadastrar|salvar/i }).click();
    await auditar(page);
  });
});
