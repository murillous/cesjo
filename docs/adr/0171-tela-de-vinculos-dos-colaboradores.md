# ADR 0171 — Histórico de vínculos na ficha do colaborador

**Status:** Aceito (2026-11-22) · Issue #51

## Contexto

Contratações e desligamentos (ADR 0170) precisam de tela. O cadastro de colaboradores já tem a
listagem e a ficha de edição (ADR 0048). Alternativas: uma página própria por vínculo, como o
desligamento do usuário atendido (`/atendidos/:id/desligar`); ou uma seção na ficha do colaborador.

## Decisão

- Componente `HistoricoVinculos` dentro da ficha do colaborador (`/colaboradores/:id`), abaixo dos
  dados: lista os vínculos do mais recente ao mais antigo, com o vínculo atual marcado na borda.
- Quem tem `COLABORADORES_GERENCIAR` vê um único botão, que muda conforme o estado: "Registrar
  desligamento" com vínculo aberto, "Registrar contratação" sem. O botão abre um formulário curto na
  própria seção (data e, no desligamento, motivo), sem trocar de página. Depois de salvar, a ficha
  recarrega o colaborador para mostrar a nova situação.
- O cadastro (`/colaboradores/novo`) ganha a data de contratação opcional.
- Mobile first como o resto do app: botões na largura toda no celular e lado a lado a partir do
  tablet.

## Consequências

- Tudo sobre o colaborador fica numa tela só, sem rota nova.
- Se o histórico crescer muito, a seção pode ganhar paginação; hoje são poucos vínculos por pessoa.
