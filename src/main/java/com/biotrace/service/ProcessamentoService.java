package com.biotrace.service;

import com.biotrace.integration.HistoricoS3Reader;
import com.biotrace.model.*;
import com.biotrace.repository.AlertaRepository;
import com.biotrace.repository.ArquivoProcessadoRepository;
import com.biotrace.repository.EstadoAlertaRepository;
import com.biotrace.repository.ParametroMetricaRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ProcessamentoService {
    private final HistoricoS3Reader reader;
    private final ParametroMetricaRepository parametroRepository;
    private final AvaliacaoService avaliacaoService;
    private final AlertasService alertasService;
    private final AlertaRepository alertaRepository;
    private final TransactionTemplate transacao;
    private final ArquivoProcessadoRepository arquivoRepository;
    private final EstadoAlertaRepository estadoRepository;

    public ProcessamentoService(HistoricoS3Reader reader, ParametroMetricaRepository parametroRepository, AvaliacaoService avaliacaoService, AlertasService alertasService, AlertaRepository alertaRepository, TransactionTemplate transacao, ArquivoProcessadoRepository arquivoRepository, EstadoAlertaRepository estadoRepository) {
        this.reader = reader;
        this.parametroRepository = parametroRepository;
        this.avaliacaoService = avaliacaoService;
        this.alertasService = alertasService;
        this.alertaRepository = alertaRepository;
        this.transacao = transacao;
        this.arquivoRepository = arquivoRepository;
        this.estadoRepository = estadoRepository;
    }

    public void processar() {
        Set<String> chavesProcessadas = arquivoRepository.buscarChavesProcessadas("biotrace-datalake");
        List<S3Object> objetosEncontrados = reader.listarObjetos();
        List<ObjetoLeitura> objetosLeituras = new ArrayList<>();

        for (S3Object objetoEncontrado : objetosEncontrados) {
            if (chavesProcessadas.contains(objetoEncontrado.key())) {
                continue;
            }

            objetosLeituras.add(prepararObjeto(objetoEncontrado));
        }

        objetosLeituras = ordenarObjetos(objetosLeituras);

        String ultimoEquipamentoBuscado = "";
        List<ParametroMetrica> parametros = new ArrayList<>();

        for (ObjetoLeitura objetoLeitura : objetosLeituras) {
            if (!objetoLeitura.getNumeroSerie().equals(ultimoEquipamentoBuscado)) {
                System.out.println("Buscando parametros do equipamento: " + objetoLeitura.getNumeroSerie());

                parametros = parametroRepository.buscarPorEquipamento(objetoLeitura.getEmpresaId(), objetoLeitura.getNumeroSerie());
                ultimoEquipamentoBuscado = objetoLeitura.getNumeroSerie();

                for (ParametroMetrica parametro : parametros) {
                    System.out.println(
                            parametro.getNomeMetrica()
                                    + " | uso: " + parametro.getLimiteUso()
                                    + " | ociosidade: " + parametro.getLimiteOciosidade()
                    );
                }

                Map<Integer, EstadoAlerta> estadosSalvos =
                        estadoRepository.buscarPorEquipamento(
                                objetoLeitura.getEmpresaId(),
                                objetoLeitura.getNumeroSerie()
                        );

                for (Map.Entry<Integer, EstadoAlerta> entrada : estadosSalvos.entrySet()) {
                    alertasService.atualizarEstado(
                            objetoLeitura.getEmpresaId(),
                            objetoLeitura.getNumeroSerie(),
                            entrada.getKey(),
                            entrada.getValue()
                    );
                }
            }

            processarArquivo(objetoLeitura, parametros);
        }

        System.out.println("Arquivos encontrados: " + objetosLeituras.size());
    }

    private void processarArquivo(
            ObjetoLeitura objetoLeitura,
            List<ParametroMetrica> parametros
    ) {
        JSONObject conteudo = reader.lerObjeto(objetoLeitura.getS3Key());

        JSONArray leituras = conteudo.getJSONArray("leituras");

        for (int i = 0; i < leituras.length(); i++) {
            JSONObject item = leituras.getJSONObject(i);
            objetoLeitura.adicionarLeitura(prepararLeitura(item));
        }

        Map<Integer, EstadoAlerta> estadosDoArquivo = new HashMap<>();
        List<String> mensagens = new ArrayList<>();

        transacao.executeWithoutResult(status -> {
            for (Leitura leitura : objetoLeitura.getLeituras()) {
                for (ParametroMetrica parametro : parametros) {
                    if ("cpu_por_nucleo".equals(parametro.getNomeMetrica())) {
                        continue;
                    }

                    if (parametro.getLimiteUso() == null && parametro.getLimiteOciosidade() == null) {
                        continue;
                    }

                    CondicaoLeitura condicao = avaliacaoService.avaliarLeitura(leitura, parametro);

                    EstadoAlerta estado = estadosDoArquivo.computeIfAbsent(
                            parametro.getIdMetrica(),
                            idMetrica -> alertasService.obterEstado(
                                    objetoLeitura.getEmpresaId(),
                                    objetoLeitura.getNumeroSerie(),
                                    idMetrica
                            ).copiar()
                    );

                    CondicaoLeitura condicaoAnterior = estado.getCondicaoAtiva();
                    AcaoAlerta acao = alertasService.registrarCondicao(estado, condicao);
                    switch (acao) {
                        case ABRIR -> {
                            Double valor = avaliacaoService.obterValor(leitura, parametro.getNomeMetrica());
                            Double limite = condicao == CondicaoLeitura.USO ? parametro.getLimiteUso() : parametro.getLimiteOciosidade();
                            Long idAlerta = alertaRepository.abrir(parametro.getIdMetrica(), condicao, leitura.getTimestamp(), limite, valor);
                            estado.setIdAlertaAtivo(idAlerta);

                            mensagens.add(
                                    "ABRIR ALERTA: "
                                            + objetoLeitura.getEmpresaId()
                                            + ":" + objetoLeitura.getNumeroSerie()
                                            + " | " + parametro.getNomeMetrica()
                                            + " | " + condicao
                                            + " | " + leitura.getTimestamp()
                            );
                        }
                        case ENCERRAR_NORMALIZACAO -> {
                            Long idAlerta = estado.getIdAlertaAtivo();
                            alertaRepository.encerrar(idAlerta, leitura.getTimestamp(), estado.getUltimaViolacaoEm(), MotivoEncerramento.NORMALIZACAO);

                            estado.setUltimaViolacaoEm(null);
                            estado.setIdAlertaAtivo(null);

                            mensagens.add(
                                    "ENCERRAR POR NORMALIZAÇÃO: "
                                            + objetoLeitura.getEmpresaId()
                                            + ":" + objetoLeitura.getNumeroSerie()
                                            + " | " + parametro.getNomeMetrica()
                                            + " | " + condicaoAnterior
                                            + " | " + leitura.getTimestamp()
                            );
                        }
                        case TROCAR_CONDICAO -> {
                            Double valor = avaliacaoService.obterValor(leitura, parametro.getNomeMetrica());
                            Double limite = condicao == CondicaoLeitura.USO ? parametro.getLimiteUso() : parametro.getLimiteOciosidade();

                            Long idAnterior = estado.getIdAlertaAtivo();

                            alertaRepository.encerrar(
                                    idAnterior,
                                    leitura.getTimestamp(),
                                    estado.getUltimaViolacaoEm(),
                                    MotivoEncerramento.MUDANCA_CONDICAO
                            );

                            Long novoId = alertaRepository.abrir(
                                    parametro.getIdMetrica(),
                                    condicao,
                                    leitura.getTimestamp(),
                                    limite,
                                    valor
                            );

                            estado.setIdAlertaAtivo(novoId);

                            mensagens.add(
                                    "TROCAR CONDIÇÃO: "
                                            + objetoLeitura.getEmpresaId()
                                            + ":" + objetoLeitura.getNumeroSerie()
                                            + " | " + parametro.getNomeMetrica()
                                            + " | de " + condicaoAnterior
                                            + " para " + condicao
                                            + " | " + leitura.getTimestamp()
                            );
                        }
                        case NENHUMA -> {}
                    }

                    boolean violacao = condicao == CondicaoLeitura.USO || condicao == CondicaoLeitura.OCIOSIDADE;
                    if (violacao && condicao == estado.getCondicaoAtiva()) {
                        estado.setUltimaViolacaoEm(leitura.getTimestamp());
                    }
                }
            }

            for (Map.Entry<Integer, EstadoAlerta> entrada : estadosDoArquivo.entrySet()) {
                EstadoAlerta estado = entrada.getValue();

                if (estado.getIdAlertaAtivo() != null) {
                    alertaRepository.atualizarUltimaViolacao(
                            estado.getIdAlertaAtivo(),
                            estado.getUltimaViolacaoEm()
                    );
                }

                estadoRepository.salvar(entrada.getKey(), estado);
            }

            arquivoRepository.registrarConclusao("biotrace-datalake", objetoLeitura.getS3Key(), Instant.now());
        });

        // Atualiza estados em memória
        for (Map.Entry<Integer, EstadoAlerta> entrada : estadosDoArquivo.entrySet()) {
            alertasService.atualizarEstado(
                    objetoLeitura.getEmpresaId(),
                    objetoLeitura.getNumeroSerie(),
                    entrada.getKey(),
                    entrada.getValue()
            );
        }

        mensagens.forEach(System.out::println);
    }

    private Leitura prepararLeitura(JSONObject item) {
        JSONObject metricas = item.getJSONObject("metricas");
        JSONObject calculados = item.getJSONObject("calculados");

        return new Leitura(
                Instant.parse(item.getString("timestamp")),
                lerDouble(metricas, "cpu_percent"),
                prepararNucleos(metricas),
                lerDouble(metricas, "temperatura_cpu"),
                lerDouble(metricas, "memoria_percent"),
                lerDouble(metricas, "memoria_disponivel_gb"),
                lerDouble(metricas, "disco_percent"),
                lerDouble(metricas, "disco_livre_gb"),
                lerDouble(metricas, "uptime_horas"),
                lerInteger(metricas, "processos_ativos"),
                lerLong(metricas, "rede_bytes_enviados"),
                lerLong(metricas, "rede_bytes_recebidos"),
                lerLong(metricas, "rede_erros_entrada"),
                lerLong(metricas, "rede_erros_saida"),
                lerDouble(calculados, "rede_bytes_enviados_por_segundo"),
                lerDouble(calculados, "rede_bytes_recebidos_por_segundo")
        );
    }

    private List<Double> prepararNucleos(JSONObject metricas) {
        if (metricas.isNull("cpu_por_nucleo")) {
            return null;
        }

        JSONArray valores = metricas.getJSONArray("cpu_por_nucleo");
        List<Double> nucleos = new ArrayList<>();
        for (int i = 0; i < valores.length(); i++) {
            nucleos.add(valores.isNull(i) ? null : valores.getDouble(i));
        }
        return nucleos;
    }

    private Double lerDouble(JSONObject objeto, String campo) {
        return objeto.isNull(campo) ? null : objeto.getDouble(campo);
    }

    private Integer lerInteger(JSONObject objeto, String campo) {
        return objeto.isNull(campo) ? null : objeto.getInt(campo);
    }

    private Long lerLong(JSONObject objeto, String campo) {
        return objeto.isNull(campo) ? null : objeto.getLong(campo);
    }

    private ObjetoLeitura prepararObjeto(S3Object objeto) {
        // ["gold", "historico", "empresa_id=", "numero_serie=", "data=", "hora=", "leituras.json]
        String[] partes = objeto.key().split("/");

        Integer empresaId = Integer.parseInt(partes[2].split("=", 2)[1]);
        String numeroSerie = partes[3].split("=", 2)[1];

        String dataStr = partes[4].split("=", 2)[1];
        Integer hora = Integer.parseInt(partes[5].split("=", 2)[1]);

        LocalDateTime dateTime = LocalDate.parse(dataStr).atTime(hora, 0);

        return new ObjetoLeitura(
                objeto.key(),
                empresaId,
                numeroSerie,
                dateTime
        );
    }

    private List<ObjetoLeitura> ordenarObjetos(List<ObjetoLeitura> objetos) {
        objetos.sort(
                Comparator.comparing(ObjetoLeitura::getNumeroSerie)
                        .thenComparing(ObjetoLeitura::getDateTime)
        );

        return objetos;
    }

}
