package lexek.wschat.db.model;

public class Emoticon {
    private Long id;
    private String code;
    private String fileName;
    private Integer height;
    private Integer width;

    public Emoticon() {
    }

    public Emoticon(Long id, String code, String fileName, Integer height, Integer width) {
        this.id = id;
        this.code = code;
        this.fileName = fileName;
        this.height = height;
        this.width = width;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getHeight() {
        return this.height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return this.width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "Emoticon{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", fileName='" + fileName + '\'' +
            ", height=" + height +
            ", width=" + width +
            '}';
    }
}
