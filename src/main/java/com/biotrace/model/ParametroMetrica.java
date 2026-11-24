package com.biotrace.model;

public class ParametroMetrica {
    private final Integer idMetrica;
    private final String nomeMetrica;
    private final Double limiteUso;
    private final Double limiteOciosidade;

    public ParametroMetrica(Integer idMetrica, String nomeMetrica, Double limiteUso, Double limiteOciosidade) {
        this.idMetrica = idMetrica;
        this.nomeMetrica = nomeMetrica;
        this.limiteUso = limiteUso;
        this.limiteOciosidade = limiteOciosidade;
    }

    public Integer getIdMetrica() {
        return idMetrica;
    }

    public String getNomeMetrica() {
        return nomeMetrica;
    }

    public Double getLimiteUso() {
        return limiteUso;
    }

    public Double getLimiteOciosidade() {
        return limiteOciosidade;
    }
}
