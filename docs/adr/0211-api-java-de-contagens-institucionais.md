# ADR 0211 — API Java de contagens do módulo Institucional

**Status:** Aceito (2026-11-23) · Issue #61

## Contexto

O relatório de transparência (ADR 0210) precisa de números de parcerias, eventos, doações e
colaboradores. O módulo Estatísticas lê os outros módulos só pelas APIs Java públicas (ADR 0195), e a
única API pública do Institucional era `Colaboradores`, feita para escolher profissionais.

Alternativas consideradas:

- **Acrescentar os métodos em `Colaboradores`**: mistura contagens de doações e eventos numa
  interface de colaboradores.
- **Uma API por assunto** (`Doacoes`, `Eventos`, `Parcerias`): mais interfaces para três ou quatro
  métodos cada, sem outro uso hoje.
- **Uma interface só de contagens do módulo**: um lugar para os números agregados, como o
  `ContagensDoPeriodo` do módulo Atendidos.

## Decisão

- Interface pública `institucional.ContagensInstitucionais`, implementada por
  `institucional.interno.ContagensDoInstitucional` em SQL agregado sobre as tabelas do módulo. Só
  conta e soma: não lê nome, descrição, contraparte nem motivo de desligamento.
- Métodos: `contarColaboradoresPorFuncaoEm(dia)`, `contarParceriasVigentes(inicio, fim)`,
  `contarArticulacoes(inicio, fim)`, `contarEventosPorTipo(inicio, fim)`,
  `contarDoacoesPorTipo(direcao, inicio, fim)` e `somarDoacoesEmDinheiro(direcao, inicio, fim)`. Os
  mapas trazem zero para a categoria sem registro.
- Colaboradores no dia: quem tem vínculo aberto naquele dia (contratado até o dia e sem desligamento
  até ele, ADR 0170). Quem não tem nenhum vínculo registrado, porque foi cadastrado antes do histórico,
  conta pela situação de hoje (ativo).
- Parcerias vigentes: parceiro ativo com vigência que alcança algum dia do período; vigência sem
  início ou sem fim fica aberta daquele lado.
- Como as outras APIs Java, não checa perfil e não audita: a permissão é de quem chama.

## Consequências

- Um relatório novo que precise de números do Institucional acrescenta um método aqui.
- A contagem de equipe de um período passado só é exata para quem tem vínculo registrado; os
  colaboradores antigos sem vínculo contam pela situação atual.
