package br.org.cesjo.sgi.pessoas.interno;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.org.cesjo.sgi.pessoas.TipoDocumento;

interface DocumentoRepositorio extends JpaRepository<Documento, UUID> {

	List<Documento> findByPessoaIdOrderByTipo(UUID pessoaId);

	Optional<Documento> findByPessoaIdAndTipo(UUID pessoaId, TipoDocumento tipo);

	boolean existsByArquivoId(UUID arquivoId);

	List<Documento> findByPessoaIdIn(Collection<UUID> pessoaIds);

}
