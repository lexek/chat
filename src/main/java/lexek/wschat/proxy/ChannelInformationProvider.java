package lexek.wschat.proxy;

import java.io.IOException;

public interface ChannelInformationProvider {
    /**
     * @return null if stream is offline, otherwise long is returned
     */
    Long fetchViewerCount() throws IOException;

    /**
     * @return null if stream is offline, otherwise {@link StreamInfo} is returned
     */
    StreamInfo fetchFullInfo() throws IOException;
}
