package lexek.wschat.util;

public class Pages {
    private Pages() {
    }

    public static int pageCount(int pageSize, int elementCount) {
        return pageSize == 0 ? 1 : (int) Math.ceil((double) elementCount / (double) pageSize);
    }
}
