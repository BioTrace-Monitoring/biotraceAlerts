# BioTrace Alerts

Aplicação Java que lê os históricos horários da Gold no Amazon S3, identifica condições fora dos limites configurados e registra os episódios de alerta no MySQL usado pela dashboard.

A captura e a ETL ficam no projeto Python. Este projeto cuida da avaliação das leituras e da persistência dos alertas.

## Como funciona

1. Consulta no MySQL quais arquivos já foram concluídos.
2. Lista os históricos do S3 e separa os arquivos pendentes.
3. Ordena os arquivos por equipamento, data e hora.
4. Busca os limites habilitados e recupera o estado salvo de cada equipamento.
5. Avalia as leituras de cada arquivo e abre, encerra ou troca a condição dos alertas.
6. Salva os alertas, os estados e a conclusão do arquivo na mesma transação.

Os parâmetros são consultados uma vez por equipamento em cada execução, antes de percorrer suas leituras.

### Origem dos dados

O bucket configurado no código é `biotrace-datalake`, na região `us-east-1`. Os arquivos seguem este padrão:

```text
gold/historico/empresa_id=2/numero_serie=BT-DEMO-0001/data=2026-11-23/hora=13/leituras.json
```

Cada documento contém um array `leituras`. Cada leitura tem `timestamp` com fuso horário, `metricas` e `calculados`.

Os históricos devem estar fechados, com leituras em ordem cronológica. O fluxo pressupõe a chegada das horas em sequência; não faz recomposição do estado para arquivos antigos enviados depois. Uma chave já registrada como concluída é ignorada, mesmo se o objeto for reescrito.

## Regras dos alertas

| Situação | Comportamento |
| --- | --- |
| Valor maior ou igual ao limite de uso | Classifica como `USO`. |
| Valor menor ou igual ao limite de ociosidade | Classifica como `OCIOSIDADE`. |
| Valor dentro dos limites configurados | Classifica como `NORMAL`. |
| Valor ausente | Classifica como `SEM_DADO`. |
| Três violações consecutivas da mesma condição | Confirma um alerta na terceira leitura. |
| Alerta continua na mesma condição | Mantém o episódio aberto, sem criar outro. |
| Três leituras normais consecutivas | Encerra o episódio por `NORMALIZACAO`. |
| Três violações consecutivas da condição oposta | Encerra o episódio anterior por `MUDANCA_CONDICAO` e abre outro. |

Um limite `null` desativa apenas aquele lado da comparação. Métricas com ambos os limites ausentes são ignoradas. `cpu_por_nucleo` não gera alertas.

`SEM_DADO` interrompe as contagens de confirmação e normalização, mas preserva um alerta já aberto. As contagens continuam entre arquivos e na virada do dia; o estado salvo permite recuperá-las após reiniciar a aplicação.

## Configuração

Requisitos:

- Java 21 e Maven.
- Acesso de leitura ao bucket: listar objetos e baixar os históricos.
- MySQL com o esquema descrito em [init.sql](../BioTrace-bd/init.sql), equipamentos, métricas e parâmetros cadastrados.
- Tabelas transacionais, como InnoDB, para garantir o rollback por arquivo.

Configure estas variáveis de ambiente no terminal ou na execução da IDE:

```bash
export DB_URL='jdbc:mysql://localhost:3306/biotrace'
export DB_USER='seu_usuario'
export DB_PASS='sua_senha'
```

As credenciais AWS são obtidas pela cadeia padrão do SDK, por exemplo, variáveis de ambiente, perfil local em `~/.aws/credentials` ou role IAM do ambiente de execução. A região está definida em `S3Config`.

O código lê as variáveis do banco com `System.getenv()`; arquivos `.env` não são carregados automaticamente.

### Tabelas utilizadas

| Tabela | Finalidade |
| --- | --- |
| `parametro_metrica` e tabelas de equipamentos/componentes | Consultar os limites habilitados de cada métrica. |
| `alerta` | Registrar confirmação, última violação e encerramento dos episódios. |
| `estado_alerta` | Guardar contagens, condições e referência ao alerta ativo. |
| `arquivo_processado` | Registrar o bucket e a chave dos arquivos concluídos. |

Os horários dos alertas são gravados em UTC. Se um arquivo falhar durante a transação, suas gravações são revertidas e ele permanece pendente para a próxima execução. Os estados em memória só são atualizados após o sucesso da transação.

## Executar

Na IDE, configure as variáveis de ambiente e execute `com.biotrace.Main`.

Pelo terminal Linux, dentro de `biotraceAlerts/`:

```bash
mvn compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" com.biotrace.Main
```

Cada chamada ao `Main` faz uma rodada de processamento dos arquivos pendentes. **O agendamento periódico ainda não está implementado.** Para acompanhar novas horas, execute novamente ou configure um agendamento externo sem sobrepor execuções.

O programa trabalha com uma instância de processamento por vez. A conclusão de arquivos e os estados persistidos são mantidos entre as execuções.

## Organização do código

| Pacote/classe | Responsabilidade |
| --- | --- |
| `Main` | Montar as dependências e iniciar o processamento. |
| `config` | Configurar os clientes S3 e MySQL. |
| `integration/HistoricoS3Reader` | Listar e baixar os históricos. |
| `service/ProcessamentoService` | Preparar, ordenar e processar os arquivos com transação por arquivo. |
| `service/AvaliacaoService` | Extrair o valor da métrica e comparar com os limites. |
| `service/AlertasService` | Controlar as sequências de leituras e as mudanças de condição. |
| `repository` | Consultar parâmetros e persistir alertas, estados e arquivos concluídos. |
| `model` | Representar leituras, parâmetros, estados e ações. |

O projeto usa AWS SDK for Java, `JSONObject`, Spring JDBC (`JdbcTemplate`), Apache Commons DBCP e MySQL Connector/J. As dependências estão no [pom.xml](pom.xml).

## Verificação manual

Execute `TesteContinuidade`, em `src/test/java`, pela IDE para conferir a continuidade da contagem entre duas referências ao mesmo estado. A saída esperada é:

```text
NENHUMA
NENHUMA
ABRIR
```

Esse exemplo usa um método `main` e valida a lógica em memória. A integração completa exige executar o processamento com S3 e MySQL configurados.
