package br.org.cesjo.sgi.pessoas.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface PessoaRepositorio extends JpaRepository<Pessoa, UUID>, JpaSpecificationExecutor<Pessoa> {
}
