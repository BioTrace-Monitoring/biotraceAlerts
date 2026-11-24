package com.biotrace.repository;

import com.biotrace.model.CondicaoLeitura;
import com.biotrace.model.EstadoAlerta;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class EstadoAlertaRepository {
    private final JdbcTemplate jdbcTemplate;

    public EstadoAlertaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<Integer, EstadoAlerta> buscarPorEquipamento(
            Integer empresaId,
            String numeroSerie
    ) {
        String sql = """
                SELECT
                    ea.fk_metrica,
                    ea.condicao_em_confirmacao,
                    ea.condicao_ativa,
                    ea.leituras_consecutivas,
                    ea.leituras_normais_consecutivas,
                    ea.ultima_violacao_em,
                    ea.fk_alerta_ativo
                FROM estado_alerta ea
                JOIN metrica m
                    ON m.id_metrica = ea.fk_metrica
                JOIN componente c
                    ON c.id_componente = m.fk_componente
                JOIN equipamento e
                    ON e.id_equipamento = c.fk_equipamento
                JOIN lote_equipamento l
                    ON l.id_lote_equipamento = e.fk_lote
                WHERE l.fk_empresa = ?
                  AND e.numero_serie = ?
                """;

        return jdbcTemplate.query(sql, rs -> {
            Map<Integer, EstadoAlerta> estados = new HashMap<>();

            while (rs.next()) {
                EstadoAlerta estado = new EstadoAlerta();
                estado.setCondicaoEmConfirmacao(
                        converterCondicao(rs.getString("condicao_em_confirmacao"))
                );
                estado.setCondicaoAtiva(
                        converterCondicao(rs.getString("condicao_ativa"))
                );
                estado.setLeiturasConsecutivas(rs.getInt("leituras_consecutivas"));
                estado.setLeiturasNormaisConsecutivas(
                        rs.getInt("leituras_normais_consecutivas")
                );
                estado.setIdAlertaAtivo(rs.getObject("fk_alerta_ativo", Long.class));

                LocalDateTime ultimaViolacao = rs.getObject(
                        "ultima_violacao_em",
                        LocalDateTime.class
                );
                estado.setUltimaViolacaoEm(
                        ultimaViolacao == null ? null : ultimaViolacao.toInstant(ZoneOffset.UTC)
                );

                estados.put(rs.getInt("fk_metrica"), estado);
            }

            return estados;
        }, empresaId, numeroSerie);
    }

    public void salvar(Integer idMetrica, EstadoAlerta estado) {
        String sql = """
                INSERT INTO estado_alerta (
                    fk_metrica,
                    condicao_em_confirmacao,
                    condicao_ativa,
                    leituras_consecutivas,
                    leituras_normais_consecutivas,
                    ultima_violacao_em,
                    fk_alerta_ativo
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    condicao_em_confirmacao = ?,
                    condicao_ativa = ?,
                    leituras_consecutivas = ?,
                    leituras_normais_consecutivas = ?,
                    ultima_violacao_em = ?,
                    fk_alerta_ativo = ?
                """;

        String emConfirmacao = estado.getCondicaoEmConfirmacao() == null
                ? null : estado.getCondicaoEmConfirmacao().name();
        String ativa = estado.getCondicaoAtiva() == null
                ? null : estado.getCondicaoAtiva().name();
        LocalDateTime ultimaViolacaoUtc = estado.getUltimaViolacaoEm() == null
                ? null : LocalDateTime.ofInstant(estado.getUltimaViolacaoEm(), ZoneOffset.UTC);

        // O mesmo estado é usado tanto na inserção quanto na atualização.
        // No upsert, 0 (sem alteração) e 2 (atualização) também são resultados válidos.
        jdbcTemplate.update(
                sql,
                idMetrica,
                emConfirmacao,
                ativa,
                estado.getLeiturasConsecutivas(),
                estado.getLeiturasNormaisConsecutivas(),
                ultimaViolacaoUtc,
                estado.getIdAlertaAtivo(),
                emConfirmacao,
                ativa,
                estado.getLeiturasConsecutivas(),
                estado.getLeiturasNormaisConsecutivas(),
                ultimaViolacaoUtc,
                estado.getIdAlertaAtivo()
        );
    }

    private CondicaoLeitura converterCondicao(String valor) {
        return valor == null ? null : CondicaoLeitura.valueOf(valor);
    }
}
