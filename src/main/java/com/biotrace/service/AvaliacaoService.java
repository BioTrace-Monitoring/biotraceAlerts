package com.biotrace.service;

import com.biotrace.model.CondicaoLeitura;
import com.biotrace.model.Leitura;
import com.biotrace.model.ParametroMetrica;

public class AvaliacaoService {
    public CondicaoLeitura avaliarLeitura(Leitura leitura, ParametroMetrica parametro) {
        Double valor = obterValor(leitura, parametro.getNomeMetrica());
        return identificarCondicao(valor, parametro);
    }

    public CondicaoLeitura identificarCondicao(Double valor, ParametroMetrica parametro) {
        if (valor == null) return CondicaoLeitura.SEM_DADO;

        if (parametro.getLimiteUso() != null && valor >= parametro.getLimiteUso()) return CondicaoLeitura.USO;
        if (parametro.getLimiteOciosidade() != null && valor <= parametro.getLimiteOciosidade()) return CondicaoLeitura.OCIOSIDADE;

        return CondicaoLeitura.NORMAL;
    }

    public Double obterValor(Leitura leitura, String nomeMetrica) {
        return switch (nomeMetrica) {
            case "cpu_percent" -> leitura.getCpuPercent();
            case "temperatura_cpu" -> leitura.getTemperaturaCpu();
            case "memoria_percent" -> leitura.getMemoriaPercent();
            case "memoria_disponivel_gb" -> leitura.getMemoriaDisponivelGb();
            case "disco_percent" -> leitura.getDiscoPercent();
            case "disco_livre_gb" -> leitura.getDiscoLivreGb();
            case "uptime_horas" -> leitura.getUptimeHoras();
            case "processos_ativos" -> paraDouble(leitura.getProcessosAtivos());
            case "rede_bytes_enviados" -> paraDouble(leitura.getRedeBytesEnviados());
            case "rede_bytes_recebidos" -> paraDouble(leitura.getRedeBytesRecebidos());
            case "rede_erros_entrada" -> paraDouble(leitura.getRedeErrosEntrada());
            case "rede_erros_saida" -> paraDouble(leitura.getRedeErrosSaida());
            case "rede_bytes_enviados_por_segundo" ->
                    leitura.getRedeBytesEnviadosPorSegundo();
            case "rede_bytes_recebidos_por_segundo" ->
                    leitura.getRedeBytesRecebidosPorSegundo();
            default -> throw new IllegalArgumentException(
                    "Métrica não suportada: " + nomeMetrica
            );
        };
    }

    private Double paraDouble(Number valor) {
        return valor == null ? null : valor.doubleValue();
    }
}
