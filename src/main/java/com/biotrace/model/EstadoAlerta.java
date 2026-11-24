package com.biotrace.model;

import java.time.Instant;

public class EstadoAlerta {
    private CondicaoLeitura condicaoEmConfirmacao;
    private CondicaoLeitura condicaoAtiva;
    private int leiturasNormaisConsecutivas;
    private int leiturasConsecutivas;
    private Instant ultimaViolacaoEm;
    private Long idAlertaAtivo;

    public EstadoAlerta copiar() {
        EstadoAlerta novo = new EstadoAlerta();
        novo.setCondicaoEmConfirmacao(this.condicaoEmConfirmacao);
        novo.setCondicaoAtiva(this.condicaoAtiva);
        novo.setLeiturasConsecutivas(this.leiturasConsecutivas);
        novo.setLeiturasNormaisConsecutivas(this.leiturasNormaisConsecutivas);
        novo.setIdAlertaAtivo(this.idAlertaAtivo);
        novo.setUltimaViolacaoEm(this.ultimaViolacaoEm);
        return novo;
    }

    public Instant getUltimaViolacaoEm() {
        return ultimaViolacaoEm;
    }

    public void setUltimaViolacaoEm(Instant ultimaViolacaoEm) {
        this.ultimaViolacaoEm = ultimaViolacaoEm;
    }

    public CondicaoLeitura getCondicaoEmConfirmacao() {
        return condicaoEmConfirmacao;
    }

    public void setCondicaoEmConfirmacao(CondicaoLeitura condicaoEmConfirmacao) {
        this.condicaoEmConfirmacao = condicaoEmConfirmacao;
    }


    public Long getIdAlertaAtivo() {
        return idAlertaAtivo;
    }

    public void setIdAlertaAtivo(Long idAlertaAtivo) {
        this.idAlertaAtivo = idAlertaAtivo;
    }

    public int getLeiturasConsecutivas() {
        return leiturasConsecutivas;
    }

    public void setLeiturasConsecutivas(int leiturasConsecutivas) {
        this.leiturasConsecutivas = leiturasConsecutivas;
    }

    public CondicaoLeitura getCondicaoAtiva() {
        return condicaoAtiva;
    }

    public void setCondicaoAtiva(CondicaoLeitura condicaoAtiva) {
        this.condicaoAtiva = condicaoAtiva;
    }

    public int getLeiturasNormaisConsecutivas() {
        return leiturasNormaisConsecutivas;
    }

    public void setLeiturasNormaisConsecutivas(int leiturasNormaisConsecutivas) {
        this.leiturasNormaisConsecutivas = leiturasNormaisConsecutivas;
    }
}
