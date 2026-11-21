package br.org.cesjo.sgi.pessoas.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PessoaRepositorio extends JpaRepository<Pessoa, UUID> {
}
