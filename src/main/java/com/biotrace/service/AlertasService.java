package com.biotrace.service;

import com.biotrace.model.AcaoAlerta;
import com.biotrace.model.CondicaoLeitura;
import com.biotrace.model.EstadoAlerta;

import java.util.HashMap;
import java.util.Map;

public class AlertasService {
    private final Map<String, EstadoAlerta> estados = new HashMap<>();

    public EstadoAlerta obterEstado(Integer empresaId, String numeroSerie, Integer idMetrica) {
        String chave = empresaId + ":" + numeroSerie + ":" + idMetrica;

        EstadoAlerta estado = estados.computeIfAbsent(chave, k -> new EstadoAlerta());
        return estado;
    }

    public AcaoAlerta registrarCondicao(EstadoAlerta alerta, CondicaoLeitura condicao) {
        if (condicao.equals(CondicaoLeitura.SEM_DADO)) {
            alerta.setCondicaoEmConfirmacao(null);
            alerta.setLeiturasConsecutivas(0);
            alerta.setLeiturasNormaisConsecutivas(0);
            return AcaoAlerta.NENHUMA;
        }

        if (condicao.equals(CondicaoLeitura.NORMAL)) {
            alerta.setCondicaoEmConfirmacao(null);
            alerta.setLeiturasConsecutivas(0);

            if (alerta.getCondicaoAtiva() != null) {
                alerta.setLeiturasNormaisConsecutivas(alerta.getLeiturasNormaisConsecutivas() + 1);
                if (alerta.getLeiturasNormaisConsecutivas() == 3) {
                    alerta.setCondicaoAtiva(null);
                    alerta.setLeiturasNormaisConsecutivas(0);
                    return AcaoAlerta.ENCERRAR_NORMALIZACAO;
                }
            } else {
                alerta.setLeiturasNormaisConsecutivas(0);
            }

            return AcaoAlerta.NENHUMA;
        }

        alerta.setLeiturasNormaisConsecutivas(0);

        if (condicao.equals(alerta.getCondicaoEmConfirmacao())) {
            alerta.setLeiturasConsecutivas(alerta.getLeiturasConsecutivas() + 1);
        } else {
            alerta.setCondicaoEmConfirmacao(condicao);
            alerta.setLeiturasConsecutivas(1);
        }

        if (alerta.getLeiturasConsecutivas() == 3) {
            if (alerta.getCondicaoAtiva() == null) {
                alerta.setCondicaoAtiva(condicao);
                alerta.setLeiturasNormaisConsecutivas(0);
                return AcaoAlerta.ABRIR;
            }

            if (alerta.getCondicaoAtiva() != condicao) {
                alerta.setLeiturasConsecutivas(0);
                alerta.setCondicaoAtiva(condicao);
                return AcaoAlerta.TROCAR_CONDICAO;
            } else {
                return AcaoAlerta.NENHUMA;
            }
        }

        return AcaoAlerta.NENHUMA;
    }

    public void atualizarEstado(Integer empresaId, String numeroSerie, Integer idMetrica, EstadoAlerta estado) {
        String chave = empresaId + ":" + numeroSerie + ":" + idMetrica;
        estados.put(chave, estado);
    }
}
