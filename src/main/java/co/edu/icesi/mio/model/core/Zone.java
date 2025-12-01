package co.edu.icesi.mio.model.core;

public class Zone {

    private final String id;
    private final String name;

    public Zone(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
