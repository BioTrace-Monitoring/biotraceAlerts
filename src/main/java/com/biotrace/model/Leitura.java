package com.biotrace.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Leitura {
    private final Instant timestamp;
    private final Double cpuPercent;
    private final List<Double> cpuPorNucleo;
    private final Double temperaturaCpu;
    private final Double memoriaPercent;
    private final Double memoriaDisponivelGb;
    private final Double discoPercent;
    private final Double discoLivreGb;
    private final Double uptimeHoras;
    private final Integer processosAtivos;
    private final Long redeBytesEnviados;
    private final Long redeBytesRecebidos;
    private final Long redeErrosEntrada;
    private final Long redeErrosSaida;
    private final Double redeBytesEnviadosPorSegundo;
    private final Double redeBytesRecebidosPorSegundo;

    public Leitura(
            Instant timestamp,
            Double cpuPercent,
            List<Double> cpuPorNucleo,
            Double temperaturaCpu,
            Double memoriaPercent,
            Double memoriaDisponivelGb,
            Double discoPercent,
            Double discoLivreGb,
            Double uptimeHoras,
            Integer processosAtivos,
            Long redeBytesEnviados,
            Long redeBytesRecebidos,
            Long redeErrosEntrada,
            Long redeErrosSaida,
            Double redeBytesEnviadosPorSegundo,
            Double redeBytesRecebidosPorSegundo
    ) {
        this.timestamp = timestamp;
        this.cpuPercent = cpuPercent;
        this.cpuPorNucleo = cpuPorNucleo == null ? null : Collections.unmodifiableList(new ArrayList<>(cpuPorNucleo));
        this.temperaturaCpu = temperaturaCpu;
        this.memoriaPercent = memoriaPercent;
        this.memoriaDisponivelGb = memoriaDisponivelGb;
        this.discoPercent = discoPercent;
        this.discoLivreGb = discoLivreGb;
        this.uptimeHoras = uptimeHoras;
        this.processosAtivos = processosAtivos;
        this.redeBytesEnviados = redeBytesEnviados;
        this.redeBytesRecebidos = redeBytesRecebidos;
        this.redeErrosEntrada = redeErrosEntrada;
        this.redeErrosSaida = redeErrosSaida;
        this.redeBytesEnviadosPorSegundo = redeBytesEnviadosPorSegundo;
        this.redeBytesRecebidosPorSegundo = redeBytesRecebidosPorSegundo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Double getCpuPercent() {
        return cpuPercent;
    }

    public List<Double> getCpuPorNucleo() {
        return cpuPorNucleo;
    }

    public Double getTemperaturaCpu() {
        return temperaturaCpu;
    }

    public Double getMemoriaPercent() {
        return memoriaPercent;
    }

    public Double getMemoriaDisponivelGb() {
        return memoriaDisponivelGb;
    }

    public Double getDiscoPercent() {
        return discoPercent;
    }

    public Double getDiscoLivreGb() {
        return discoLivreGb;
    }

    public Double getUptimeHoras() {
        return uptimeHoras;
    }

    public Integer getProcessosAtivos() {
        return processosAtivos;
    }

    public Long getRedeBytesEnviados() {
        return redeBytesEnviados;
    }

    public Long getRedeBytesRecebidos() {
        return redeBytesRecebidos;
    }

    public Long getRedeErrosEntrada() {
        return redeErrosEntrada;
    }

    public Long getRedeErrosSaida() {
        return redeErrosSaida;
    }

    public Double getRedeBytesEnviadosPorSegundo() {
        return redeBytesEnviadosPorSegundo;
    }

    public Double getRedeBytesRecebidosPorSegundo() {
        return redeBytesRecebidosPorSegundo;
    }
}
