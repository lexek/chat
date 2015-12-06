package lexek.wschat.util;

public class Pages {
    private Pages() {
    }

    public static int pageCount(int pageSize, int elementCount) {
        return pageSize == 0 ? 1 : (int) Math.ceil((double) elementCount / (double) pageSize);
    }

    public static String escapeSearch(String input) {
        String search = input;
        search = search.replace("!", "!!");
        search = search.replace("%", "!%");
        search = search.replace("_", "!_");
        search = '%' + search + '%';
        return search;
    }
}
