package br.org.cesjo.sgi;

import org.springframework.boot.SpringApplication;

public class TestSgiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(SgiBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
