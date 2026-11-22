# ADR 0197 — Gráficos em HTML e CSS, sem biblioteca, e área de relatórios com abas

**Status:** Aceito (2026-11-22) · Issue #56

## Contexto

Os dashboards (RF017) pedem gráficos nas cores do tema. Os dados são poucas categorias com uma
contagem cada: seis tipos de deficiência, três turnos, dois a quatro gêneros. O app roda bem no
celular (RNF 3.1) e passa por auditoria de acessibilidade com axe no CI (ADR 0123).

Alternativas consideradas:

- **Chart.js ou ECharts**: muitos tipos de gráfico, mas somam dezenas a centenas de KB ao pacote,
  desenham em canvas (o leitor de tela não lê sem trabalho extra) e as cores precisam ser passadas
  em JavaScript, fora dos tokens CSS.
- **ngx-charts ou outra biblioteca em SVG para Angular**: segue o Angular, mas depende de D3 e da
  versão do Angular, que o projeto atualiza cedo.
- **SVG próprio**: leve, mas o texto em SVG não quebra linha, e rótulos longos ("Síndrome de Down
  (T21)") ficam difíceis no celular.
- **Barras horizontais em HTML e CSS**: o rótulo é texto comum, que quebra linha e escala com a
  fonte; a barra é um elemento com largura proporcional; as cores vêm dos tokens `--sgi-*`.

## Decisão

- Um componente `app-grafico-barras` (frontend, `modulos/relatorios/grafico-barras.ts`) desenha uma
  série de barras horizontais em HTML e CSS, sem biblioteca.
- A barra usa `--sgi-color-primary`; a barra "Não informado" usa `--sgi-color-accent-soft`, mais
  apagada. O turquesa tem contraste 2,8:1 com o fundo, abaixo de 3:1, então cada barra leva o valor e
  a porcentagem escritos na ponta, em cor de texto, e a cor nunca é o único jeito de ler o número.
- A barra mais longa ocupa a faixa toda; as outras são proporcionais a ela. Barra de 20px de
  espessura, base reta e ponta arredondada.
- Abaixo de cada gráfico, "Ver como tabela" abre a mesma informação numa tabela. A lista de barras é
  escondida do leitor de tela (`aria-hidden`), que lê a tabela.
- No celular o rótulo fica em cima da barra; do tablet em diante, numa coluna à esquerda. No desktop
  os gráficos ficam dois por linha.
- A área "Relatórios" é uma página com abas (`mat-tab-nav-bar`) e uma rota por aba. As abas ficam
  numa lista só, em `modulos/relatorios/abas.ts`.

## Consequências

- Gráfico de linha ou de pizza, se algum relatório pedir, entra como componente próprio ou traz uma
  biblioteca nessa hora, com novo ADR.
- Um dashboard novo é uma linha em `abas.ts`, uma rota e um componente que usa `app-grafico-barras`.
