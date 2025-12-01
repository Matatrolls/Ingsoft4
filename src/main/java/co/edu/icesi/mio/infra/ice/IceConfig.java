package co.edu.icesi.mio.infra.ice;

public final class IceConfig {

    public static final String ADAPTER_NAME = "AdminChannelAdapter";
    public static final String IDENTITY = "adminChannel";
    public static final String HOST = "localhost";
    public static final int PORT = 10000;

    private IceConfig() {
    }

    public static String endpoints() {
        return "default -h " + HOST + " -p " + PORT;
    }

    public static String proxyString(String hostOverride) {
        String h = (hostOverride == null || hostOverride.isBlank()) ? HOST : hostOverride;
        return IDENTITY + ":default -h " + h + " -p " + PORT;
    }
}
