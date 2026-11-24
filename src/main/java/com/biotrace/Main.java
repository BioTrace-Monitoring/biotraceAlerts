package com.biotrace;

import com.biotrace.config.DBConnectionProvider;
import com.biotrace.config.S3Config;
import com.biotrace.integration.HistoricoS3Reader;
import com.biotrace.repository.AlertaRepository;
import com.biotrace.repository.ArquivoProcessadoRepository;
import com.biotrace.repository.EstadoAlertaRepository;
import com.biotrace.repository.ParametroMetricaRepository;
import com.biotrace.service.AlertasService;
import com.biotrace.service.AvaliacaoService;
import com.biotrace.service.ProcessamentoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.*;


public class Main {
    public static void main(String[] args) {
        try {
            HistoricoS3Reader reader = new HistoricoS3Reader(S3Config.getClient());

            DBConnectionProvider banco = new DBConnectionProvider();

            JdbcTemplate jdbcTemplate = banco.getConnection();
            var gerenciador = new DataSourceTransactionManager(jdbcTemplate.getDataSource());
            var transacao = new TransactionTemplate(gerenciador);

            ParametroMetricaRepository parametroRepository = new ParametroMetricaRepository(jdbcTemplate);
            AlertaRepository alertaRepository = new AlertaRepository(jdbcTemplate);
            ArquivoProcessadoRepository arquivoRepository = new ArquivoProcessadoRepository(jdbcTemplate);
            EstadoAlertaRepository estadoRepository = new EstadoAlertaRepository(jdbcTemplate);

            AvaliacaoService avaliacaoService = new AvaliacaoService();
            AlertasService alertasService = new AlertasService();

            ProcessamentoService processamentoService = new ProcessamentoService(
                    reader,
                    parametroRepository,
                    avaliacaoService,
                    alertasService,
                    alertaRepository,
                    transacao,
                    arquivoRepository,
                    estadoRepository
            );

            processamentoService.processar();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        } catch (SdkClientException e) {
            System.err.println("O SDK não conseguiu se comunicar com o servidor... Verifique as credenciais!");
            System.exit(1);
        }
    }
}
