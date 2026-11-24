import com.biotrace.model.CondicaoLeitura;
import com.biotrace.model.EstadoAlerta;
import com.biotrace.service.AlertasService;

public class TesteContinuidade {
    public static void main(String[] args) {
        AlertasService service = new AlertasService();

        // Simula duas violações no arquivo A.
        EstadoAlerta estadoA = service.obterEstado(2, "BT-DEMO-0001", 1);

        System.out.println(service.registrarCondicao(
                estadoA, CondicaoLeitura.USO));

        System.out.println(service.registrarCondicao(
                estadoA, CondicaoLeitura.USO));

        // Simula a primeira leitura do arquivo B.
        EstadoAlerta estadoB = service.obterEstado(2, "BT-DEMO-0001", 1);

        System.out.println(service.registrarCondicao(
                estadoB, CondicaoLeitura.USO));
    }
}