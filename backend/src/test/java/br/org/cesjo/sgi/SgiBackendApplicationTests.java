package br.org.cesjo.sgi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SgiBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
