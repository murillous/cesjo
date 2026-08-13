package br.org.cesjo.sgi.repository;

import br.org.cesjo.sgi.domain.ListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Integer> {

    @Query("""
            select l from ListaEspera l
            join fetch l.beneficiario b
            where l.dataSaida is null
            order by l.dataEntrada asc, l.id asc
            """)
    List<ListaEspera> listarAtivasOrdenadas();

    @Query("""
            select l from ListaEspera l
            join fetch l.beneficiario b
            where l.dataSaida is null and b.id = :beneficiarioId
            """)
    Optional<ListaEspera> buscarAtivaPorBeneficiario(Integer beneficiarioId);

    long countByDataSaidaIsNull();
}
