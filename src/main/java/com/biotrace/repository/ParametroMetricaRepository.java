package com.biotrace.repository;

import com.biotrace.model.ParametroMetrica;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class ParametroMetricaRepository {
    private final JdbcTemplate jdbcTemplate;

    public ParametroMetricaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ParametroMetrica> buscarPorEquipamento(Integer empresaId, String numeroSerie) {
        String sql = """
                SELECT
                    m.id_metrica,
                    tm.tipo_metrica AS nome_metrica,
                    pm.limite_uso,
                    pm.limite_ociosidade
                FROM parametro_metrica pm
                JOIN metrica m ON m.id_metrica = pm.fk_metrica
                JOIN tipo_metrica tm ON tm.id_tipo_metrica = m.fk_tipo_metrica
                JOIN componente c ON c.id_componente = m.fk_componente
                JOIN equipamento e ON e.id_equipamento = c.fk_equipamento
                JOIN lote_equipamento l ON l.id_lote_equipamento = e.fk_lote
                WHERE l.fk_empresa = ?
                    AND e.numero_serie = ?
                    AND pm.habilitado = TRUE
                """;

        return jdbcTemplate.query(
                sql,
                (rs, numeroLinha) -> {
                    var uso = rs.getBigDecimal("limite_uso");
                    var ociosidade = rs.getBigDecimal("limite_ociosidade");

                    return new ParametroMetrica(
                            rs.getInt("id_metrica"),
                            rs.getString("nome_metrica"),
                            uso == null ? null : uso.doubleValue(),
                            ociosidade == null ? null : ociosidade.doubleValue()
                    );
                },
                empresaId,
                numeroSerie
        );
    }
}
