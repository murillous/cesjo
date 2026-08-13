package br.org.cesjo.sgi.service;

import br.org.cesjo.sgi.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegraBeneficiarioTest {

    @Test
    void aceitaIdadeEntreDezoitoECinquentaENoveAnos() {
        LocalDate entrada = LocalDate.of(2026, 8, 13);

        assertDoesNotThrow(() -> RegraBeneficiario.validarIdade(LocalDate.of(2008, 8, 13), entrada));
        assertDoesNotThrow(() -> RegraBeneficiario.validarIdade(LocalDate.of(1966, 8, 14), entrada));
    }

    @Test
    void rejeitaPessoaForaDaFaixaEtaria() {
        LocalDate entrada = LocalDate.of(2026, 8, 13);

        assertThrows(RegraNegocioException.class,
                () -> RegraBeneficiario.validarIdade(LocalDate.of(2008, 8, 14), entrada));
        assertThrows(RegraNegocioException.class,
                () -> RegraBeneficiario.validarIdade(LocalDate.of(1966, 8, 13), entrada));
    }

    @Test
    void normalizaTelefoneComPontuacao() {
        assertEquals("98991234567", RegraBeneficiario.normalizarTelefone("(98) 99123-4567"));
    }

    @Test
    void rejeitaNascimentoFuturoETelefoneComCaracteresInvalidos() {
        LocalDate nascimentoFuturo = LocalDate.now().plusDays(1);

        assertThrows(RegraNegocioException.class,
                () -> RegraBeneficiario.validarIdade(nascimentoFuturo, nascimentoFuturo.plusYears(18)));
        assertThrows(RegraNegocioException.class,
                () -> RegraBeneficiario.normalizarTelefone("abc 98 99123-4567"));
    }
}
