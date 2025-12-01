package co.edu.icesi.mio.infra.ice;

import co.edu.icesi.mio.infra.csv.GrafoMIO;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class AdminChannelServer {

    private final GrafoMIO grafo;
    private Communicator communicator;

    public AdminChannelServer(GrafoMIO grafo) {
        this.grafo = grafo;
    }

    public void start() {
        communicator = Util.initialize();
        ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                IceConfig.ADAPTER_NAME,
                IceConfig.endpoints()
        );

        AdminChannelServant servant = new AdminChannelServant(grafo);

        adapter.add(servant, Util.stringToIdentity(IceConfig.IDENTITY));
        adapter.activate();

        System.out.println("[ICE][SERVER] AdminChannel listo en " + IceConfig.endpoints());
    }

    public void waitForShutdown() {
        if (communicator != null) {
            communicator.waitForShutdown();
        }
    }

    public void shutdown() {
        if (communicator != null) {
            communicator.shutdown();
            communicator = null;
        }
    }
}
