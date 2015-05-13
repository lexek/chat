package lexek.wschat.db.model;

import java.util.List;

public class DataPage<T> {
    private final List<T> data;
    private final int page;
    private final int pageCount;

    public DataPage(List<T> data, int page, int pageCount) {
        this.data = data;
        this.page = page;
        this.pageCount = pageCount;
    }

    public List<T> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return pageCount;
    }
}
