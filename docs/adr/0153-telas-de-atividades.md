# ADR 0153 — Telas de atividades: diário por data e ficha de registro

**Status:** Aceito (2026-11-22) · Issue #45

## Contexto

A equipe registra atividades depois de realizadas e procura por "o que fizemos naquele dia". As telas
precisam funcionar no celular (RNF 3.1) e seguir o padrão das telas de escuta e visitas.

Alternativas consideradas: tabela com colunas (não cabe no celular); cartões em grade (a ordem por
data se perde); escolher participantes por busca com autocompletar (o serviço tem poucas dezenas de
usuários ativos, e o seletor múltiplo do Material já é acessível por teclado).

## Decisão

- Módulo `frontend/src/app/modulos/atividades`, item de menu "Atividades" (ícone `groups`), exigindo
  `ATIVIDADES_LER`.
- `/atividades` é um diário: lista ordenada da atividade mais recente para a mais antiga, cada item
  com a data num bloco à esquerda (dia grande, mês e ano pequenos), título, tipo e os primeiros nomes
  dos participantes ("Ana, Bruno e mais 3"). Paginado como as outras listas.
- `/atividades/nova` (exige `ATIVIDADES_REGISTRAR`) e `/atividades/:id` usam a mesma ficha: leitura
  com descrição e lista de participantes, e botão "Corrigir atividade" para quem registra.
- Participantes e profissionais são seletores múltiplos. Na correção, quem já estava na atividade
  continua na lista mesmo se foi desligado ou ficou inativo.
- O campo de título avisa para não escrever dado sigiloso, porque o título aparece na listagem.
- SCSS mobile first, só `min-width` nas media queries; tokens do tema, sem HEX.

## Consequências

- Os filtros por tipo, período, profissional e usuário (issue #47) entram acima do diário.
