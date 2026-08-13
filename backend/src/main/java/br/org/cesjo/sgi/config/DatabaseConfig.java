package br.org.cesjo.sgi.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    @Bean
    ApplicationRunner verificarForeignKeys(JdbcTemplate jdbcTemplate) {
        return args -> {
            Integer foreignKeys = jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class);
            if (!Integer.valueOf(1).equals(foreignKeys)) {
                throw new IllegalStateException("As chaves estrangeiras do SQLite não estão ativas");
            }
        };
    }
}
