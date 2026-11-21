package br.org.cesjo.sgi.atendidos.interno;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.cesjo.sgi.atendidos.FaixaEtaria;
import br.org.cesjo.sgi.compartilhado.web.RecursoNaoEncontradoException;
import br.org.cesjo.sgi.listaespera.ListaEspera;
import br.org.cesjo.sgi.pessoas.DadosPessoa;
import br.org.cesjo.sgi.pessoas.DadosResponsavel;
import br.org.cesjo.sgi.pessoas.Pessoas;

/** Listagem e ficha dos usuários atendidos (RF007, ADR 0083). A permissão é conferida no controlador. */
@Service
@Transactional(readOnly = true)
class ServicoConsultaAtendidos {

	/** "Hoje" da idade e da faixa etária é o dia em Brasília, não em UTC. */
	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	private final ConsultaAtendidos consulta;

	private final InsercaoRepositorio insercoes;

	private final Pessoas pessoas;

	private final ListaEspera listaEspera;

	private final Clock relogio = Clock.system(FUSO);

	ServicoConsultaAtendidos(ConsultaAtendidos consulta, InsercaoRepositorio insercoes, Pessoas pessoas,
			ListaEspera listaEspera) {
		this.consulta = consulta;
		this.insercoes = insercoes;
		this.pessoas = pessoas;
		this.listaEspera = listaEspera;
	}

	Page<VisoesAtendido.Basico> listarBasico(FiltroAtendidos filtro, Pageable pagina) {
		return consulta.listar(filtro, hoje(), pagina).map(VisoesAtendido.Basico::de);
	}

	Page<VisoesAtendido.Item> listarCompleto(FiltroAtendidos filtro, Pageable pagina) {
		LocalDate hoje = hoje();
		return consulta.listar(filtro, hoje, pagina).map(linha -> VisoesAtendido.Item.de(linha, hoje));
	}

	VisoesAtendido.Basico fichaBasica(UUID id) {
		Insercao insercao = insercao(id);
		DadosPessoa pessoa = pessoa(insercao);
		return new VisoesAtendido.Basico(insercao.getId(), pessoa.nomeCompleto(), insercao.getSituacao(),
				insercao.getTurno(), insercao.getDataInsercao(), false);
	}

	VisoesAtendido.Ficha fichaCompleta(UUID id) {
		Insercao insercao = insercao(id);
		DadosPessoa pessoa = pessoa(insercao);
		LocalDate hoje = hoje();
		LocalDate nascimento = pessoa.dataNascimento();
		String cuidador = pessoas.responsaveis(pessoa.id())
			.stream()
			.filter(responsavel -> responsavel.id().equals(insercao.getCuidadorResponsavelId()))
			.map(DadosResponsavel::nomeCompleto)
			.findFirst()
			.orElse(null);
		VisoesAtendido.Inscricao inscricao = listaEspera.obter(insercao.getInscricaoId())
			.map(i -> new VisoesAtendido.Inscricao(i.id(), i.dataCadastro(), i.origemEncaminhamento()))
			.orElse(null);
		return new VisoesAtendido.Ficha(insercao.getId(), pessoa.id(), pessoa.nomeCompleto(), insercao.getSituacao(),
				insercao.getTurno(), insercao.getDataInsercao(), pessoa.tipoDeficiencia(), nascimento,
				nascimento == null ? null : FaixaEtaria.idade(nascimento, hoje),
				FaixaEtaria.de(nascimento, hoje).orElse(null), pessoa.telefone(), cuidador, inscricao, true);
	}

	private Insercao insercao(UUID id) {
		return insercoes.findById(id)
			.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário atendido não encontrado."));
	}

	private DadosPessoa pessoa(Insercao insercao) {
		return pessoas.obter(insercao.getPessoaId())
			.orElseThrow(() -> new IllegalStateException("Inserção sem pessoa: " + insercao.getPessoaId()));
	}

	private LocalDate hoje() {
		return LocalDate.now(relogio);
	}

}
