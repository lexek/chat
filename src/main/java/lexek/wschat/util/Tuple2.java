package lexek.wschat.util;

public class Tuple2<L, R> {
    private final L l;
    private final R r;


    public Tuple2(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public L getL() {
        return l;
    }

    public R getR() {
        return r;
    }
}
