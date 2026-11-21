package br.org.cesjo.sgi.listaespera.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface InscricaoRepositorio extends JpaRepository<InscricaoListaEspera, UUID> {
}
