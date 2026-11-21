package br.org.cesjo.sgi.arquivos.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ArquivoRepository extends JpaRepository<Arquivo, UUID> {

}
