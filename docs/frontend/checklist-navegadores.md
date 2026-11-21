# Checklist de navegadores e acessibilidade

Conferência manual dos fluxos da lista de espera (RNF 3.1, issue #24). O CI já audita as telas
principais com axe e percorre os formulários com o teclado em Chromium (celular e desktop), WebKit
(iPhone) e Firefox ([ADR 0123](../adr/0123-auditoria-de-acessibilidade-no-ci.md)). Esta lista cobre o
que a automação não pega: o Safari de verdade num iPhone, o leitor de tela e o toque.

Faça a checklist antes de cada versão que mexa em telas e anote o resultado na tabela do fim. Use só
dados fictícios (LGPD): nada de nomes, telefones ou laudos reais.

## Onde conferir

| Navegador | Aparelho | Como |
|---|---|---|
| Safari | iPhone com o iOS atual e o anterior | aparelho real; o simulador do Xcode não substitui o teclado virtual nem o VoiceOver |
| Chrome | Android | aparelho real ou o modo dispositivo do Chrome no computador |
| Chrome, Edge e Firefox | computador | janela com 1280 px e com 375 px de largura |

## Fluxos

Para cada navegador, marque cada item.

### Entrar e trocar a senha

- [ ] A tela de entrada abre sem rolagem lateral, com o logo inteiro.
- [ ] O teclado virtual do iPhone não cobre o campo focado nem o botão Entrar.
- [ ] O login não ganha letra maiúscula nem correção automática.
- [ ] Senha errada mostra a mensagem de erro, e o VoiceOver a lê.
- [ ] No primeiro acesso, a troca de senha aparece e aceita o gerenciador de senhas do iOS.

### Lista de espera

- [ ] A barra inferior mostra Início, Fila e os outros módulos, e o item atual fica destacado.
- [ ] O VoiceOver lê o item da barra como "Fila, Lista de espera".
- [ ] A lista rola sem a barra inferior tremer ou sumir (Safari esconde e mostra a barra de endereço).
- [ ] O conteúdo não fica atrás da barra inferior nem da área do indicador de início do iPhone.

### Cadastro de candidato

- [ ] Todos os campos têm rótulo visível, e o VoiceOver lê o rótulo ao focar.
- [ ] Ao tocar num campo perto da base da tela, ele sobe acima do teclado virtual e da barra inferior.
- [ ] O campo de telefone abre o teclado numérico.
- [ ] O seletor de data abre, aceita a data digitada (dd/mm/aaaa) e fecha com o botão Voltar do VoiceOver.
- [ ] As listas de opções (tipo de deficiência, origem do encaminhamento) abrem e fecham com toque e
      com o VoiceOver.
- [ ] Enviar vazio mostra os erros embaixo de cada campo, e o VoiceOver avisa.
- [ ] Depois de cadastrar, aparece a confirmação e o foco vai para ela ou para o título da página.

### Teclado (computador)

- [ ] Tab percorre os campos e botões na ordem da tela, sem pular nem prender o foco.
- [ ] O foco aparece sempre, com borda azul petróleo, inclusive em botões e listas de opções.
- [ ] Enter envia os formulários; Esc fecha o seletor de data e as listas de opções.
- [ ] Com zoom de 200%, nada se sobrepõe e não aparece rolagem lateral.

### Movimento e contraste

- [ ] Com "Reduzir movimento" ligado (Ajustes, Acessibilidade, Movimento), não há animações além do
      indispensável.
- [ ] Com "Texto maior" no nível máximo do iOS, os textos crescem e continuam legíveis.

## Registro

| Data | Versão (commit) | Navegador e aparelho | Resultado | Problemas abertos | Quem |
|---|---|---|---|---|---|
| | | | | | |
