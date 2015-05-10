package lexek.wschat.proxy;

public class StreamInfo {
    private final long streamId;
    private final long started;
    private final String title;
    private long viewers;

    public StreamInfo(long streamId, long started, String title, long viewers) {
        this.streamId = streamId;
        this.started = started;
        this.title = title;
        this.viewers = viewers;
    }

    public long getStreamId() {
        return streamId;
    }

    public long getStarted() {
        return started;
    }

    public String getTitle() {
        return title;
    }

    public long getViewers() {
        return viewers;
    }

    public void setViewers(long viewers) {
        this.viewers = viewers;
    }
}
