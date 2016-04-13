package lexek.wschat.db.model;

public class EmoticonCount {
    private final Emoticon emoticon;
    private final long count;

    public EmoticonCount(Emoticon emoticon, long count) {
        this.emoticon = emoticon;
        this.count = count;
    }

    public Emoticon getEmoticon() {
        return emoticon;
    }

    public long getCount() {
        return count;
    }
}
