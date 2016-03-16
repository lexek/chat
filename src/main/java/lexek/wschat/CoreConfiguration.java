package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoreConfiguration {
    private final int poolSize;
    private final int wsPort;
    private final int maxEmoticonHeight;
    private final int maxEmoticonWidth;
    private final String graphiteServer;
    private final String graphitePrefix;
    private final String host;
    private final String dataDir;
    private final String title;

    public CoreConfiguration(@JsonProperty("poolSize") int poolSize,
                             @JsonProperty("wsPort") int wsPort,
                             @JsonProperty("maxEmoticonHeight") int maxEmoticonHeight,
                             @JsonProperty("maxEmoticonWidth") int maxEmoticonWidth,
                             @JsonProperty("graphiteServer") String graphiteServer,
                             @JsonProperty("graphitePrefix") String graphitePrefix,
                             @JsonProperty("host") String host,
                             @JsonProperty("dataDir") String dataDir,
                             @JsonProperty("title") String title) {
        this.poolSize = poolSize;
        this.wsPort = wsPort;
        this.maxEmoticonHeight = maxEmoticonHeight;
        this.maxEmoticonWidth = maxEmoticonWidth;
        this.graphiteServer = graphiteServer;
        this.graphitePrefix = graphitePrefix;
        this.host = host;
        this.dataDir = dataDir;
        this.title = title;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getWsPort() {
        return wsPort;
    }

    public int getMaxEmoticonHeight() {
        return maxEmoticonHeight;
    }

    public int getMaxEmoticonWidth() {
        return maxEmoticonWidth;
    }

    public String getHost() {
        return host;
    }

    public String getDataDir() {
        return dataDir;
    }

    public String getGraphiteServer() {
        return graphiteServer;
    }

    public String getGraphitePrefix() {
        return graphitePrefix;
    }

    public String getTitle() {
        return title;
    }
}
