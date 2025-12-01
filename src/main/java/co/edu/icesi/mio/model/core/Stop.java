package co.edu.icesi.mio.model.core;

public class Stop {

    private final String id;
    private final String name;

    public Stop(String id, String name) {
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
