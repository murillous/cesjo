package br.org.cesjo.sgi.service;

import br.org.cesjo.sgi.exception.RegraNegocioException;

import java.time.LocalDate;
import java.time.Period;

public final class RegraBeneficiario {

    private RegraBeneficiario() {
    }

    public static void validarIdade(LocalDate dataNascimento, LocalDate dataEntrada) {
        if (dataNascimento.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de nascimento não pode estar no futuro");
        }
        if (dataNascimento.isAfter(dataEntrada)) {
            throw new RegraNegocioException("A data de nascimento não pode ser posterior à data de entrada");
        }

        int idade = Period.between(dataNascimento, dataEntrada).getYears();
        if (idade < 18 || idade > 59) {
            throw new RegraNegocioException("O beneficiário deve ter entre 18 e 59 anos na data de entrada");
        }
    }

    public static String normalizarTelefone(String telefone) {
        String valor = telefone == null ? "" : telefone.trim();
        if (!valor.matches("(?:\\d{10,11}|\\(\\d{2}\\) ?\\d{4,5}-\\d{4})")) {
            throw new RegraNegocioException("O telefone deve usar 10 ou 11 dígitos, com DDD");
        }
        return valor.replaceAll("\\D", "");
    }
}
