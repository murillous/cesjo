package br.org.cesjo.sgi.atendidos.interno;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ViolacaoRepositorio extends JpaRepository<ViolacaoDireito, UUID> {
}
