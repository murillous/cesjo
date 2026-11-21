# Guia de Interface Frontend

## 1. Objetivo

Este documento define o padrão visual e as principais regras de implementação da interface frontend do sistema.

O objetivo é garantir consistência entre páginas, componentes e funcionalidades, permitindo que diferentes desenvolvedores implementem novas telas mantendo a mesma identidade visual.

A interface deverá seguir uma identidade predominantemente clara, utilizando branco como base, tons de verde/turquesa como cores institucionais e tons verde-amarelados como cores de destaque.

---

## 2. Referência visual

A identidade visual deverá seguir como referência o site apresentado no layout fornecido.

As principais características observadas são:

* predominância de fundos brancos e off-white;
* utilização de verde/turquesa na identidade institucional;
* utilização de verde-amarelado como cor de destaque;
* textos e elementos de maior contraste em verde escuro;
* visual limpo, com bastante espaço em branco;
* navegação simples e horizontal;
* componentes com poucos efeitos visuais;
* destaque de elementos através de cor, e não através de excesso de bordas ou sombras.

---

## 3. Paleta de cores

A aplicação deverá utilizar cores através de **tokens semânticos**, evitando inserir valores HEX diretamente em cada componente.

| Token                  | Cor aproximada | Utilização                                          |
| ---------------------- | -------------: | --------------------------------------------------- |
| `background-primary`   |      `#FFFFFF` | Fundo principal da aplicação                        |
| `background-secondary` |      `#F1F0E8` | Seções secundárias e fundos suaves                  |
| `primary`              |      `#10AAA6` | Verde/turquesa institucional                        |
| `primary-dark`         |      `#34545C` | Textos fortes, botões e elementos de alto contraste |
| `accent`               |      `#F2EA65` | Destaques e elementos de atenção                    |
| `accent-soft`          |      `#D2D180` | Fundos, badges e destaques suaves                   |
| `text-primary`         |      `#34545C` | Texto principal                                     |
| `text-secondary`       |      `#6B6B6B` | Texto secundário                                    |
| `border`               |      `#DBDDD7` | Bordas e divisórias                                 |
| `white`                |      `#FFFFFF` | Texto sobre fundos escuros e superfícies            |

### Distribuição visual recomendada

A interface deverá utilizar aproximadamente:

**60–70% — Branco / Off-white**

Utilizado como fundo principal para manter a interface limpa e leve.

**20–30% — Verde/turquesa**

Utilizado na identidade da aplicação, botões, ícones, links e elementos institucionais.

**5–10% — Verde-amarelado**

Utilizado apenas para destaques, indicadores, hover, elementos selecionados ou chamadas importantes.

O verde-amarelado não deverá ocupar grandes áreas da interface.

---

## 4. Regras de utilização das cores

O fundo padrão das páginas deverá ser branco (`#FFFFFF`).

Seções que precisem de diferenciação visual poderão utilizar o fundo secundário (`#F1F0E8`).

A cor `primary` (`#10AAA6`) deverá representar a identidade institucional e poderá ser utilizada em:

* ícones;
* detalhes visuais;
* links;
* elementos gráficos;
* indicadores;
* componentes selecionados.

Para textos, botões importantes e elementos que necessitem maior contraste, deverá ser utilizado `primary-dark` (`#34545C`).

A cor `accent` (`#F2EA65`) deverá ser utilizada de maneira controlada em:

* underline de item ativo;
* badges;
* indicadores;
* hover;
* detalhes gráficos;
* chamadas de atenção.

### Acessibilidade

O verde/turquesa `#10AAA6` não apresenta contraste suficiente com branco para textos pequenos.

Portanto:

**Evitar**

Texto `#10AAA6` sobre `#FFFFFF`.

**Preferir**

Texto `#34545C` sobre `#FFFFFF`.

ou

Texto `#FFFFFF` sobre `#34545C`.

O contraste deverá seguir, sempre que possível, as recomendações **WCAG nível AA**, utilizando relação mínima de aproximadamente:

* `4.5:1` para textos normais;
* `3:1` para textos grandes e determinados elementos de interface.

---

## 5. Design Tokens

As cores não deverão ser chamadas diretamente pelo seu nome visual dentro dos componentes.

Evitar nomes como:

`verde`

`verdeClaro`

`verdeEscuro`

Preferir nomes relacionados à função:

`primary`

`primaryDark`

`backgroundPrimary`

`backgroundSecondary`

`textPrimary`

`textSecondary`

`border`

`accent`

Isso permite alterar a identidade visual futuramente sem modificar todos os componentes da aplicação.

---

## 6. Tipografia

A aplicação deverá possuir uma família tipográfica definida globalmente.

A captura de tela não é suficiente para determinar com precisão a fonte original da marca. Portanto, a fonte oficial deverá ser confirmada com o material de identidade visual.

Enquanto não existir uma fonte institucional definida, a aplicação usa **Atkinson Hyperlegible Next** em todos os textos. Ela foi desenhada pelo Braille Institute para leitura com baixa visão: distingue bem letras e números parecidos (como `0` e `O`, `1`, `l` e `I`). A escolha combina com um centro de referência da pessoa com deficiência. Ver ADR 0036 (provisório, pergunta 16 do planejamento).

Deverão existir níveis tipográficos padronizados. Os tamanhos partem do celular; só o Heading 1 cresce a partir do desktop.

| Elemento  | Tamanho (celular) | A partir de 992px | Peso recomendado | Utilização                    |
| --------- | ----------------: | ----------------: | ---------------: | ----------------------------- |
| Heading 1 |              30px |              36px |          600–700 | Título principal              |
| Heading 2 |              24px |              24px |              600 | Títulos de seções             |
| Heading 3 |              20px |              20px |          500–600 | Subtítulos                    |
| Body      |              17px |              17px |              400 | Texto padrão                  |
| Small     |              15px |              15px |              400 | Informações auxiliares        |
| Legenda   |              13px |              13px |          400–600 | Rótulos da barra de navegação |
| Button    |                 — |                 — |          500–600 | Botões                        |

Evitar definir tamanhos de fonte individualmente em cada página.

---

## 7. Espaçamento

A aplicação deverá utilizar uma escala de espaçamento consistente.

Escala recomendada:

`4px`

`8px`

`12px`

`16px`

`24px`

`32px`

`48px`

`64px`

O valor base recomendado é **8px**.

Exemplo:

Espaço pequeno entre ícone e texto: `8px`.

Espaço interno de um card: `16px` ou `24px`.

Espaço entre seções: `32px`, `48px` ou `64px`.

Essa abordagem evita valores arbitrários como `13px`, `27px` ou `43px` espalhados pela aplicação.

---

## 8. Componentes

Componentes reutilizáveis deverão ser criados para elementos que aparecem diversas vezes no sistema.

Entre eles:

* Button;
* Input;
* Select;
* Textarea;
* Checkbox;
* Radio;
* Card;
* Modal;
* Alert;
* Badge;
* Navbar;
* Sidebar;
* Table;
* Pagination;
* Breadcrumb;
* Tooltip;
* Loading;
* Empty State.

Cada componente deverá possuir comportamento e aparência padronizados.

Por exemplo, todos os botões primários deverão possuir a mesma:

altura;

cor;

tipografia;

borda;

border-radius;

hover;

focus;

disabled.

---

## 9. Estados dos componentes

Todo componente interativo deverá definir pelo menos os seguintes estados:

**Default**

Estado padrão.

**Hover**

Quando o usuário posiciona o cursor sobre o componente.

**Focus**

Quando o elemento recebe foco através do teclado ou formulário.

**Active**

Durante a interação.

**Disabled**

Quando a ação não está disponível.

**Loading**

Quando uma operação está sendo processada.

Quando aplicável, também deverão existir:

**Success**

**Warning**

**Error**

---

## 10. Botões

A aplicação deverá possuir categorias claras de botões.

### Primary

Utilizado para a ação principal de uma tela.

Cor recomendada:

`#34545C`

Texto:

`#FFFFFF`

### Secondary

Utilizado para ações secundárias.

Pode utilizar fundo branco, texto verde escuro e borda.

### Accent

Utilizado somente quando houver necessidade de destaque especial.

A cor verde-amarelada deverá ser utilizada com moderação.

### Danger

Ações destrutivas, como excluir registros, deverão utilizar uma cor específica de erro e não uma das cores institucionais.

---

## 11. Navegação

A navegação deverá seguir o comportamento apresentado na referência:

menu visualmente simples;

fundo claro;

itens com boa área de clique;

item ativo claramente identificado;

utilização controlada da cor de destaque.

A navegação principal muda de formato conforme a largura da tela:

* **celular (até 767px):** barra de navegação inferior, no alcance do polegar, com até cinco destinos. Se houver mais de cinco módulos, os quatro primeiros ficam na barra e o restante vai para o item "Mais", que abre uma folha inferior (bottom sheet) com a lista;
* **tablet (768px a 991px):** trilho lateral com todos os módulos, ícone sobre o rótulo;
* **desktop (992px ou mais):** menu lateral com ícone e rótulo na mesma linha.

Na barra inferior cada item tem um quinto da largura da tela. Módulos com nome longo devem definir um rótulo curto (por exemplo, "Fila" para "Lista de espera"). O nome completo continua sendo o nome acessível do link.

O item ativo é marcado por um **disco** na cor `accent` atrás do ícone, retirado do selo circular da marca, e pelo rótulo em peso 600. O mesmo disco é usado na página inicial para destacar o dia de hoje. Esse é o único uso do disco: ele não deve virar decoração em outros componentes.

---

## 12. Bordas e sombras

A identidade visual deverá priorizar uma interface limpa.

Bordas deverão utilizar tons neutros suaves.

Recomendação:

`1px solid #DBDDD7`

Sombras deverão ser utilizadas somente quando forem necessárias para representar elevação, como:

modais;

menus flutuantes;

dropdowns;

cards destacados.

Evitar sombras fortes em todos os componentes.

---

## 13. Border Radius

O sistema deverá utilizar uma escala limitada de arredondamentos.

Sugestão:

`4px` — pequenos elementos;

`8px` — inputs e botões;

`12px` — cards;

`16px` — componentes maiores.

Não utilizar diferentes valores de border-radius arbitrariamente em cada tela.

---

## 14. Responsividade

A aplicação deverá ser desenvolvida com abordagem **mobile first** (ADR 0035). A equipe técnica usa o sistema no celular durante visitas domiciliares, escutas e contatos; o computador fica para relatórios e tarefas administrativas.

Na prática:

* os estilos base valem para o celular;
* telas maiores são tratadas com media queries `min-width`, usando os breakpoints globais (`768px`, `992px`, `1440px`);
* não usar media queries `max-width`;
* alvos de toque com no mínimo `48px`;
* respeitar as áreas seguras do aparelho (`env(safe-area-inset-*)`) nas barras fixas;
* uma ação principal por tela.

Os componentes devem se adaptar pelo menos aos seguintes contextos:

**Mobile**

Telas pequenas e smartphones.

**Tablet**

Telas intermediárias.

**Desktop**

Notebooks e monitores.

**Large Desktop**

Monitores de maior resolução.

O layout não deverá depender exclusivamente de tamanhos fixos em pixels.

Devem ser priorizados:

Flexbox;

CSS Grid;

larguras máximas;

unidades relativas;

breakpoints definidos globalmente.

---

## 15. Ícones

Deverá existir uma única biblioteca principal de ícones para toda a aplicação.

Não misturar diferentes estilos de ícones sem necessidade.

Todos os ícones deverão possuir:

tamanho consistente;

espessura visual semelhante;

alinhamento adequado;

cor definida pelo Design System.

---

## 16. Imagens

Imagens institucionais e banners deverão respeitar:

proporção original;

responsividade;

qualidade adequada;

otimização para web.

Sempre que possível deverão ser utilizados formatos modernos, como WebP ou AVIF.

Elementos gráficos da identidade da organização deverão utilizar arquivos oficiais da marca.

---

## 17. Acessibilidade

A interface deverá considerar acessibilidade desde a implementação.

Os principais requisitos incluem:

contraste adequado;

navegação por teclado;

estado de foco visível;

labels em campos de formulário;

texto alternativo em imagens relevantes;

HTML semântico;

uso correto de headings;

feedback visual para erros;

não utilizar somente a cor para transmitir uma informação.

---

## 18. Organização do frontend

Os estilos deverão ser centralizados.

Valores relacionados ao Design System não deverão ser duplicados entre páginas.

A aplicação deverá possuir uma camada responsável por:

cores;

tipografia;

espaçamento;

breakpoints;

bordas;

sombras;

z-index;

componentes compartilhados.

As páginas deverão consumir esses padrões ao invés de recriá-los.

---

## 19. Regra principal de desenvolvimento

Antes de criar um novo estilo ou componente, o desenvolvedor deverá verificar se já existe um padrão equivalente no Design System.

Um novo padrão só deverá ser criado quando o existente não atender ao requisito da interface.

Isso reduz duplicação, melhora a manutenção e mantém consistência visual em toda a aplicação.

---

## 20. Resultado esperado

Todas as telas deverão aparentar fazer parte do mesmo produto.

Independentemente do desenvolvedor responsável pela implementação, elementos equivalentes devem possuir:

mesma aparência;

mesmo comportamento;

mesmos espaçamentos;

mesmas cores;

mesma tipografia;

mesmos estados de interação.

O Design System será a referência principal para decisões relacionadas à interface.
