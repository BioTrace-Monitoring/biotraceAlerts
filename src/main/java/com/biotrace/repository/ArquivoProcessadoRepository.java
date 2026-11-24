package com.biotrace.repository;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

public class ArquivoProcessadoRepository {
    private final JdbcTemplate jdbcTemplate;

    public ArquivoProcessadoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> buscarChavesProcessadas(String bucket) {
        String sql = """
                SELECT s3_key FROM arquivo_processado
                WHERE bucket = ?
                """;

        return new HashSet<>(
                jdbcTemplate.queryForList(sql, String.class, bucket)
        );
    }

    public void registrarConclusao(String bucket, String s3Key, Instant processadoEm) {
        String sql = """
        INSERT INTO arquivo_processado (
            bucket,
            s3_key,
            processado_em
        )
        VALUES (?, ?, ?)
        """;

        LocalDateTime processadoUtc = LocalDateTime.ofInstant(processadoEm, ZoneOffset.UTC);
        int linhasAfetadas = jdbcTemplate.update(
                sql,
                bucket,
                s3Key,
                processadoUtc
        );

        if (linhasAfetadas != 1) {
            throw new IllegalStateException(
                    "Não foi possível registrar a conclusão do arquivo."
            );
        }
    }
}
