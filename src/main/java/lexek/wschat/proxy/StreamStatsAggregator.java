package lexek.wschat.proxy;

import com.codahale.metrics.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamStatsAggregator implements Gauge<StreamInfo> {
    private final Logger logger = LoggerFactory.getLogger(StreamStatsAggregator.class);
    private List<ChannelInformationProvider> providers = new ArrayList<>();
    private ChannelInformationProvider primaryChannelInformationProvider = null;

    public void addPlatform(ChannelInformationProvider channelInformationProvider, boolean primary) {
        if (primary && primaryChannelInformationProvider == null) {
            this.primaryChannelInformationProvider = channelInformationProvider;
        } else {
            this.providers.add(channelInformationProvider);
        }
    }

    @Override
    public StreamInfo getValue() {
        if (primaryChannelInformationProvider != null) {
            StreamInfo result = null;
            try {
                result = primaryChannelInformationProvider.fetchFullInfo();
            } catch (Exception e) {
                logger.warn("exception occurred while fetching primary channel information", e);
            }

            if (result != null) {
                for (ChannelInformationProvider provider : providers) {
                    Long value = null;
                    try {
                        value = provider.fetchViewerCount();
                    } catch (IOException e) {
                        logger.warn("exception occurred while fetching viewer count", e);
                    }
                    if (value != null) {
                        result.setViewers(result.getViewers() + value);
                    }
                }
            }
            return result;
        } else {
            return null;
        }
    }
}
