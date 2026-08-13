package br.org.cesjo.sgi.repository;

import br.org.cesjo.sgi.domain.Beneficiario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiarioRepository extends JpaRepository<Beneficiario, Integer> {
}
