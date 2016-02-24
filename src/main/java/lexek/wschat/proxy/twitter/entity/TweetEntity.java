package lexek.wschat.proxy.twitter.entity;

import org.jetbrains.annotations.NotNull;

public abstract class TweetEntity implements Comparable<TweetEntity> {
    private final int start;
    private final int end;

    public TweetEntity(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getEnd() {
        return end;
    }

    public int getStart() {
        return start;
    }

    @Override
    public int compareTo(@NotNull TweetEntity o) {
        return this.start - o.start;
    }

    public abstract void render(StringBuilder stringBuilder);
}
