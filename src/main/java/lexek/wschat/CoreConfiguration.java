package lexek.wschat;

public class CoreConfiguration {
    private final int poolSize;
    private final int wsPort;
    private final String host;
    private final String dataDir;

    public CoreConfiguration(int poolSize, int wsPort, String host, String dataDir) {
        this.poolSize = poolSize;
        this.wsPort = wsPort;
        this.host = host;
        this.dataDir = dataDir;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getWsPort() {
        return wsPort;
    }

    public String getHost() {
        return host;
    }

    public String getDataDir() {
        return dataDir;
    }
}
