package co.edu.icesi.mio.app;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import co.edu.icesi.mio.infra.ice.AdminChannelServer;
import co.edu.icesi.mio.service.analytics.MioGraphRepository;

import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) throws IOException {

        MioGraphRepository repo = new MioGraphRepository();

        GrafoMIO grafo = repo.getGrafo();

        System.out.println("Grafo cargado en memoria. Paradas: "
                + grafo.getParadas().size()
                + ", Rutas: " + grafo.getRutas().size()
                + ", Arcos: " + grafo.getArcos().size());

        AdminChannelServer iceServer = new AdminChannelServer(grafo);
        iceServer.start();

        iceServer.waitForShutdown();
    }
}
