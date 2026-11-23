package br.org.cesjo.sgi.demonstracao;

import java.util.List;
import java.util.Random;

/**
 * Listas de nomes, endereços e trechos de texto da carga de demonstração. Tudo é fictício: os nomes são
 * combinações aleatórias de nomes e sobrenomes comuns, e as instituições não existem.
 */
final class Catalogo {

	static final List<String> NOMES_FEMININOS = List.of("Ana", "Beatriz", "Camila", "Daniela", "Eduarda",
			"Fernanda", "Gabriela", "Helena", "Isabela", "Juliana", "Larissa", "Luana", "Mariana", "Natália",
			"Patrícia", "Raquel", "Sabrina", "Tatiane", "Vanessa", "Yasmin", "Aline", "Bruna", "Carolina", "Débora",
			"Elaine", "Flávia", "Giovana", "Jéssica", "Letícia", "Marta", "Priscila", "Rosana", "Sônia", "Viviane");

	static final List<String> NOMES_MASCULINOS = List.of("André", "Bruno", "Carlos", "Diego", "Eduardo", "Felipe",
			"Gabriel", "Gustavo", "Henrique", "Igor", "João", "Leonardo", "Lucas", "Marcelo", "Mateus", "Nicolas",
			"Otávio", "Paulo", "Rafael", "Rodrigo", "Samuel", "Thiago", "Vinícius", "Wagner", "Alexandre", "Caio",
			"Davi", "Enzo", "Fábio", "Heitor", "Júlio", "Márcio", "Renato", "Sérgio");

	static final List<String> SOBRENOMES = List.of("Silva", "Souza", "Oliveira", "Santos", "Pereira", "Lima",
			"Ferreira", "Almeida", "Costa", "Rodrigues", "Gomes", "Martins", "Araújo", "Barbosa", "Ribeiro",
			"Carvalho", "Rocha", "Dias", "Moreira", "Nascimento", "Teixeira", "Mendes", "Cardoso", "Campos",
			"Vieira", "Monteiro", "Freitas", "Duarte", "Fonseca", "Machado", "Lopes", "Batista", "Reis", "Pinto");

	/** Bairros fictícios, para nenhum endereço da demonstração coincidir com um endereço real. */
	static final List<String> BAIRROS = List.of("Jardim das Acácias", "Vila Esperança", "Parque dos Ipês",
			"Santa Clara", "São Benedito", "Bela Vista", "Nova Aurora", "Recanto Verde", "Jardim Primavera",
			"Vila Operária", "Alto do Cruzeiro", "Morada do Sol", "Vale do Sol", "Monte Alegre", "Boa Esperança",
			"Jardim Colonial", "Santa Luzia", "Vila Nova", "Cidade Jardim", "Parque Industrial");

	static final List<String> LOGRADOUROS = List.of("Rua das Flores", "Rua São José", "Avenida Brasil",
			"Rua Sete de Setembro", "Rua Tiradentes", "Rua Dom Pedro II", "Rua da Paz", "Rua Esperança",
			"Avenida dos Andradas", "Rua Santa Rita", "Rua Marechal Deodoro", "Rua Padre Anchieta",
			"Travessa das Hortênsias", "Rua dos Ipês", "Rua Bom Jesus", "Rua Rio Branco", "Rua Quinze de Novembro");

	static final String CIDADE = "Cidade Exemplo";

	static final List<String> MEDICAMENTOS = List.of("Risperidona 1 mg, uma vez ao dia",
			"Carbamazepina 200 mg, duas vezes ao dia", "Ácido valproico 500 mg, à noite",
			"Fluoxetina 20 mg, pela manhã", "Levotiroxina 50 mcg, em jejum", "Baclofeno 10 mg, três vezes ao dia",
			"Metilfenidato 10 mg, pela manhã", "Clonazepam 0,5 mg, à noite");

	static final List<String> OBSERVACOES_SAUDE = List.of(
			"Acompanhamento neurológico semestral na rede pública. Crises convulsivas controladas.",
			"Usa cadeira de rodas e precisa de apoio nas transferências.",
			"Restrição alimentar: sem lactose. Acompanhamento com nutricionista.",
			"Hipersensibilidade a ruídos; evitar ambientes com muito barulho.",
			"Acompanhamento fonoaudiológico semanal. Comunicação por gestos e pranchas.",
			"Hipertensão controlada com medicação. Aferir pressão nas atividades físicas.",
			"Baixa visão; precisa de material com fonte ampliada.",
			"Sem intercorrências de saúde recentes.");

	static final List<String> DINAMICAS_FAMILIARES = List.of(
			"Mora com a mãe, principal cuidadora, e dois irmãos mais novos. O pai visita aos fins de semana.",
			"Mora com os avós maternos desde a infância. A avó concentra os cuidados e relata cansaço.",
			"Família de cinco pessoas em casa alugada. A mãe trabalha como diarista e o pai está desempregado.",
			"Mora com a mãe e o padrasto. Relação familiar estável, com divisão dos cuidados entre os dois.",
			"Mora com uma irmã mais velha após o falecimento da mãe. A irmã trabalha em horário comercial.",
			"Família extensa no mesmo terreno; tios e primos ajudam nos cuidados durante a semana.");

	static final List<String> NECESSIDADES = List.of(
			"Acesso a transporte adaptado para chegar ao serviço.",
			"Atualização do Cadastro Único e orientação sobre o BPC.",
			"Acompanhamento em saúde mental para a cuidadora principal.",
			"Inserção em atividades de convivência fora de casa.",
			"Apoio para regularizar a documentação civil.",
			"Orientação à família sobre rotina e autonomia nas atividades diárias.");

	static final List<String> HABILIDADES = List.of(
			"Comunica-se bem verbalmente e reconhece cores e formas.",
			"Realiza a higiene pessoal com pouca ajuda.",
			"Gosta de música e participa de atividades em grupo.",
			"Tem boa coordenação motora fina e gosta de desenhar.",
			"Usa o celular para mensagens e chamadas de vídeo com a família.",
			"Ajuda nas tarefas domésticas simples, como arrumar a mesa.");

	static final List<String> POTENCIALIDADES = List.of(
			"Interesse por jardinagem e atividades ao ar livre.",
			"Boa memória para rotinas e horários.",
			"Facilidade de vínculo com outras pessoas do grupo.",
			"Interesse em aprender a usar o transporte público com apoio.",
			"Família participativa e disposta a seguir as orientações da equipe.",
			"Gosta de cozinhar e pode desenvolver autonomia na preparação de lanches.");

	static final List<String> OBJETIVOS_PIA = List.of("Ampliar a autonomia nas atividades de vida diária",
			"Fortalecer os vínculos familiares e comunitários", "Garantir o acesso aos benefícios socioassistenciais",
			"Reduzir a sobrecarga da cuidadora principal", "Ampliar a participação em atividades de convivência",
			"Desenvolver a comunicação e a expressão de vontades", "Garantir o acompanhamento contínuo em saúde");

	static final List<String> ACOES_PIA = List.of("Oficinas semanais de atividades de vida diária",
			"Encaminhamento ao CRAS de referência para atualização do Cadastro Único",
			"Grupo mensal de famílias com orientação da equipe técnica",
			"Articulação com a unidade básica de saúde do território",
			"Atendimento individual quinzenal com a psicologia",
			"Participação nas oficinas de música e expressão corporal");

	static final List<String> OBSERVACOES_VISITA = List.of(
			"Casa com acessibilidade parcial; banheiro sem barras de apoio. Família receptiva à equipe.",
			"Visita realizada com a mãe e a avó. Rotina organizada, mas a cuidadora relata sobrecarga.",
			"Moradia em área de difícil acesso, com escadas. Discutida a necessidade de transporte adaptado.",
			"Família relata melhora no comportamento desde o início das atividades no serviço.",
			"Ambiente doméstico adequado. Combinada nova visita para acompanhar o PIA.",
			"A família pediu orientação sobre o BPC; agendado atendimento com a assistente social.");

	static final List<String> MOTIVOS_CANCELAMENTO = List.of("Família não estava em casa no horário combinado.",
			"Cuidadora pediu para remarcar por consulta médica.", "Chuva forte impediu o deslocamento da equipe.",
			"Veículo do serviço indisponível no dia.");

	static final List<String> OBSERVACOES_SAIDA = List.of(
			"Família informou que conseguiu vaga em outro serviço do município.",
			"Três tentativas de contato sem retorno, por telefone e por visita.",
			"Avaliação da equipe indicou perfil para outro serviço da rede.",
			"Família mudou de cidade.");

	static final List<String> OBSERVACOES_DESLIGAMENTO = List.of(
			"Família mudou para outro município e foi orientada a procurar o serviço de referência local.",
			"Objetivos do PIA alcançados; usuário segue em atividades da rede de convivência.",
			"Faltas seguidas sem justificativa apesar das tentativas de contato da equipe.",
			"Família pediu o desligamento para conciliar a rotina de trabalho da cuidadora.",
			"Usuário passou a frequentar curso profissionalizante em horário integral.");

	static final List<String> DESCRICOES_VIOLACAO = List.of(
			"Relato de que o usuário passa a maior parte do dia trancado em casa enquanto a família trabalha.",
			"Cuidadora relata exaustão e dificuldade em manter os cuidados de higiene e alimentação.",
			"Benefício do usuário administrado por terceiro sem prestação de contas à família.",
			"Usuário relatou ser impedido de participar de atividades fora de casa.",
			"Equipe observou sinais de descuido com a medicação de uso contínuo.",
			"Relato de agressões verbais recorrentes no ambiente familiar.",
			"Familiares desconsideram as escolhas do usuário nas decisões do dia a dia.");

	static final List<String> ATIVIDADES_GRUPO = List.of("Oficina de música", "Oficina de culinária",
			"Roda de conversa", "Oficina de artesanato", "Passeio ao parque municipal", "Oficina de jardinagem",
			"Cine debate", "Expressão corporal", "Jogos cooperativos", "Oficina de pintura");

	static final List<String> ATIVIDADES_FAMILIA = List.of("Grupo de famílias", "Encontro de cuidadores",
			"Palestra sobre direitos da pessoa com deficiência", "Orientação sobre benefícios",
			"Roda de cuidado com quem cuida");

	static final List<String> ATIVIDADES_MISTAS = List.of("Festa junina", "Confraternização de fim de ano",
			"Piquenique com as famílias", "Mostra de trabalhos das oficinas", "Dia das famílias");

	static final List<String> DESCRICOES_ATIVIDADE = List.of(
			"Atividade realizada conforme o planejamento, com boa participação do grupo.",
			"O grupo trabalhou a cooperação e a escuta. Dois participantes precisaram de apoio individual.",
			"Atividade adaptada para os participantes com mobilidade reduzida. Retorno positivo das famílias.",
			"Encontro com troca de experiências entre as famílias sobre a rotina de cuidados.",
			"Participantes demonstraram autonomia crescente nas etapas da atividade.");

	static final List<String> DESCRICOES_ATENDIMENTO = List.of(
			"Atendimento para acompanhamento das metas do PIA. Usuário relata satisfação com as oficinas.",
			"Escuta da cuidadora sobre dificuldades na rotina. Orientações sobre organização dos horários.",
			"Atendimento conjunto com psicologia e serviço social para discutir encaminhamentos.",
			"Avaliação funcional de atividades de vida diária com a terapia ocupacional.");

	static final List<String> DESCRICOES_ESTUDO_CASO = List.of(
			"Estudo de caso com a equipe para revisar o PIA diante de mudanças na dinâmica familiar.",
			"Discussão de caso com a rede para alinhar o acompanhamento em saúde e assistência.");

	static final List<String> INSTITUICOES = List.of("CRAS Norte", "CRAS Sul", "CRAS Leste", "CREAS Regional",
			"Unidade Básica de Saúde Vila Nova", "CAPS Regional", "Escola Municipal Monte Alegre",
			"Secretaria Municipal de Assistência Social", "Conselho Municipal dos Direitos da Pessoa com Deficiência",
			"Defensoria Pública", "Promotoria de Justiça", "Policlínica Municipal");

	static final List<String> ASSUNTOS_CONTATO = List.of("Confirmação de frequência", "Retorno sobre encaminhamento",
			"Agendamento de atendimento", "Atualização de documentos", "Informações sobre o serviço",
			"Retorno de faltas", "Orientação sobre transporte", "Solicitação de relatório");

	static final List<String> ASSUNTOS_OFICIO = List.of("Encaminhamento para avaliação",
			"Solicitação de acompanhamento conjunto", "Relatório de acompanhamento", "Declaração de frequência",
			"Solicitação de vaga em transporte adaptado", "Resposta a ofício", "Pedido de informações do caso");

	static final List<String> DESCRICOES_COMUNICACAO = List.of("Contato realizado por telefone, sem pendências.",
			"Família informou que o usuário está doente e faltará esta semana.",
			"Instituição confirmou o recebimento e retornará em até 15 dias.",
			"Orientação prestada e registrada no prontuário.");

	record Parceiro(String nome, String tipo, String instrumento) {
	}

	static final List<Parceiro> PARCEIROS = List.of(
			new Parceiro("Secretaria Municipal de Assistência Social", "ORGAO_PUBLICO", "Termo de colaboração 012/2025"),
			new Parceiro("Secretaria Municipal de Saúde", "ORGAO_PUBLICO", "Acordo de cooperação técnica"),
			new Parceiro("Secretaria Municipal de Educação", "ORGAO_PUBLICO", null),
			new Parceiro("Associação Mãos que Acolhem", "ORGANIZACAO_SOCIEDADE_CIVIL", "Termo de parceria"),
			new Parceiro("Instituto Caminhar Juntos", "ORGANIZACAO_SOCIEDADE_CIVIL", null),
			new Parceiro("Rede Solidária do Bairro", "ORGANIZACAO_SOCIEDADE_CIVIL", null),
			new Parceiro("Supermercado Bom Preço Exemplo", "EMPRESA", "Doação mensal de alimentos"),
			new Parceiro("Transportes Rota Certa Exemplo", "EMPRESA", "Contrato de transporte adaptado"),
			new Parceiro("Faculdade Exemplo de Ciências da Saúde", "INSTITUICAO_ENSINO", "Convênio de estágio"),
			new Parceiro("Universidade Exemplo — Curso de Terapia Ocupacional", "INSTITUICAO_ENSINO",
					"Projeto de extensão"),
			new Parceiro("Grupo de Voluntários Esperança", "OUTRO", null),
			new Parceiro("Clube de Serviço Exemplo", "OUTRO", null));

	static final List<String> ARTICULACOES = List.of("Reunião para alinhar o fluxo de encaminhamentos",
			"Visita técnica para conhecer o serviço", "Planejamento conjunto de oficina temática",
			"Renovação do termo de parceria", "Discussão de casos compartilhados", "Entrega de relatório semestral",
			"Organização de evento comunitário");

	static final List<String> DESCRICOES_EVENTO = List.of("Planejamento mensal das atividades do serviço",
			"Formação sobre comunicação alternativa e aumentativa", "Prestação de contas à direção",
			"Visita de monitoramento do órgão gestor", "Apresentação do serviço para a rede socioassistencial",
			"Seminário municipal sobre inclusão", "Revisão dos fluxos de atendimento", "Formação em primeiros socorros");

	static final List<String> DOADORES = List.of("Supermercado Bom Preço Exemplo", "Paróquia Exemplo",
			"Grupo de Voluntários Esperança", "Clube de Serviço Exemplo", "Loja de Móveis Exemplo",
			"Padaria Pão Nosso Exemplo", "Doador anônimo", "Empresa Exemplo de Tecnologia", "Associação Mãos que Acolhem");

	static final List<String> BENEFICIARIOS_DOACAO = List.of("Famílias atendidas pelo serviço",
			"Associação Mãos que Acolhem", "Rede Solidária do Bairro", "CRAS Norte");

	private final Random sorteio;

	Catalogo(Random sorteio) {
		this.sorteio = sorteio;
	}

	<T> T um(List<T> opcoes) {
		return opcoes.get(sorteio.nextInt(opcoes.size()));
	}

	String sobrenomes() {
		String primeiro = um(SOBRENOMES);
		String segundo = um(SOBRENOMES);
		while (segundo.equals(primeiro)) {
			segundo = um(SOBRENOMES);
		}
		return primeiro + " " + segundo;
	}

	String nome(boolean feminino, String sobrenomes) {
		return um(feminino ? NOMES_FEMININOS : NOMES_MASCULINOS) + " " + sobrenomes;
	}

	/** Celular ao acaso, sempre com DDD 99 e o nono dígito. */
	String telefone() {
		return "999" + (10_000_000 + sorteio.nextInt(90_000_000));
	}

	/** CPF com dígitos verificadores válidos, gerado ao acaso. */
	String cpf() {
		StringBuilder digitos = new StringBuilder();
		for (int i = 0; i < 9; i++) {
			digitos.append(sorteio.nextInt(10));
		}
		digitos.append(digitoCpf(digitos, 9));
		digitos.append(digitoCpf(digitos, 10));
		return digitos.toString();
	}

	private static int digitoCpf(CharSequence digitos, int posicao) {
		int soma = 0;
		for (int i = 0; i < posicao; i++) {
			soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
		}
		int resto = soma * 10 % 11;
		return resto == 10 ? 0 : resto;
	}

	String nis() {
		return "1" + (1_000_000_000L + sorteio.nextLong(9_000_000_000L));
	}

	String rg() {
		return "MG-" + (10 + sorteio.nextInt(9)) + "." + (100 + sorteio.nextInt(900)) + "."
				+ (100 + sorteio.nextInt(900));
	}

	String cep() {
		return "99" + (100_000 + sorteio.nextInt(900_000));
	}

}
