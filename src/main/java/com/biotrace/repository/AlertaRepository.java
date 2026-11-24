package com.biotrace.repository;

import com.biotrace.model.CondicaoLeitura;
import com.biotrace.model.MotivoEncerramento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AlertaRepository {
    private final JdbcTemplate jdbcTemplate;

    public AlertaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long abrir(Integer idMetrica, CondicaoLeitura tipoAlerta, Instant confirmadoEm, Double limiteAplicado, Double valorConfirmacao) {
        String sql = """
        INSERT INTO alerta (
            fk_metrica,
            tipo_alerta,
            confirmado_em,
            ultima_violacao_em,
            limite_aplicado,
            valor_confirmacao
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        KeyHolder chaveGerada = new GeneratedKeyHolder();
        LocalDateTime dataUtc = LocalDateTime.ofInstant(confirmadoEm, ZoneOffset.UTC);

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            statement.setInt(1, idMetrica);
            statement.setString(2, tipoAlerta.name());
            statement.setObject(3, dataUtc);
            statement.setObject(4, dataUtc);
            statement.setDouble(5, limiteAplicado);
            statement.setDouble(6, valorConfirmacao);

            return statement;
        }, chaveGerada);

        Number id = chaveGerada.getKey();
        if (id == null) {
            throw new IllegalStateException(
                    "O banco não retornou o ID do alerta criado."
            );
        }

        return id.longValue();
    }

    public void encerrar(
            Long idAlerta,
            Instant encerradoEm,
            Instant ultimaViolacaoEm,
            MotivoEncerramento motivo
    ) {
        String sql = """ 
       UPDATE alerta
        SET encerrado_em = ?,
            motivo_encerramento = ?,
            ultima_violacao_em = ?
        WHERE id_alerta = ?
          AND encerrado_em IS NULL
        """;

        LocalDateTime encerradoEmUtc = LocalDateTime.ofInstant(encerradoEm, ZoneOffset.UTC);
        LocalDateTime ultimaViolacaoUtc = LocalDateTime.ofInstant(ultimaViolacaoEm, ZoneOffset.UTC);

        int linhasAfetadas = jdbcTemplate.update(sql, encerradoEmUtc, motivo.name(), ultimaViolacaoUtc, idAlerta);
        if (linhasAfetadas != 1) {
            throw new IllegalStateException("Não encontramos um alerta aberto correspondente ao ID");
        }
    }

    public void atualizarUltimaViolacao(Long idAlerta, Instant ultimaViolacaoEm) {
        String sql = """
                UPDATE alerta
                SET ultima_violacao_em = ?
                WHERE id_alerta = ?
                  AND encerrado_em IS NULL
                """;

        LocalDateTime ultimaViolacaoUtc = LocalDateTime.ofInstant(ultimaViolacaoEm, ZoneOffset.UTC);
        int linhasAfetadas = jdbcTemplate.update(sql, ultimaViolacaoUtc, idAlerta);
        if (linhasAfetadas != 1) {
            throw new IllegalStateException("Não encontramos um alerta aberto correspondente ao ID");
        }
    }
}
