package br.org.cesjo.testes.criptografia;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.org.cesjo.sgi.compartilhado.criptografia.TextoCifradoConverter;

/**
 * Entidade só de teste para o {@link TextoCifradoConverter}. Fica fora de {@code br.org.cesjo.sgi} para
 * não entrar na varredura de entidades da aplicação, que valida o esquema contra as migrações.
 */
@Entity
@Table(name = "teste_campo_cifrado")
public class RegistroComCampoCifrado {

	@Id
	private UUID id;

	@Column(name = "relato")
	@Convert(converter = TextoCifradoConverter.class)
	private String relato;

	protected RegistroComCampoCifrado() {
	}

	public RegistroComCampoCifrado(UUID id, String relato) {
		this.id = id;
		this.relato = relato;
	}

	public UUID getId() {
		return id;
	}

	public String getRelato() {
		return relato;
	}

}
