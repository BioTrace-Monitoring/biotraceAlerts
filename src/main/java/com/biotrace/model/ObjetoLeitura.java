package com.biotrace.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ObjetoLeitura {
    private final String s3Key;
    private final Integer empresaId;
    private final String numeroSerie;
    private final LocalDateTime dateTime;
    private List<Leitura> leituras = new ArrayList<>();

    public ObjetoLeitura(String s3Key, Integer empresaId, String numeroSerie, LocalDateTime dateTime) {
        this.s3Key = s3Key;
        this.empresaId = empresaId;
        this.numeroSerie = numeroSerie;
        this.dateTime = dateTime;
    }

    public String getS3Key() {
        return s3Key;
    }

    public Integer getEmpresaId() {
        return empresaId;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setLeituras(List<Leitura> leituras) {
        this.leituras = leituras;
    }

    public void adicionarLeitura(Leitura leitura) {
        leituras.add(leitura);
    }

    public List<Leitura> getLeituras() {
        return leituras;
    }

    @Override
    public String toString() {
        return "ObjetoLeitura{" +
                "s3Key='" + s3Key + '\'' +
                ", empresaId=" + empresaId +
                ", numeroSerie='" + numeroSerie + '\'' +
                ", dateTime=" + dateTime +
                '}';
    }
}
