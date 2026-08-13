package br.org.cesjo.sgi.repository;

import br.org.cesjo.sgi.domain.ContaAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Integer> {
    Optional<ContaAcesso> findByEmail(String email);
}
