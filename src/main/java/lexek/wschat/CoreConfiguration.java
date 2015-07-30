package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoreConfiguration {
    private final int poolSize;
    private final int wsPort;
    private final String host;
    private final String dataDir;

    public CoreConfiguration(@JsonProperty("poolSize") int poolSize,
                             @JsonProperty("wsPort") int wsPort,
                             @JsonProperty("host") String host,
                             @JsonProperty("dataDir") String dataDir) {
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
