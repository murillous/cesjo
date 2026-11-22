package br.org.cesjo.sgi.pessoas.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CadastroComplementarRepositorio extends JpaRepository<CadastroComplementarPessoa, UUID> {
}
