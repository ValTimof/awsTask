package enums;

public enum TableDescription {
    PARTITION("config", "S"),
    SORT("bucketName", "N"),
    SECONDATY_INDEX_1("filePath", "S"),
    SECONDATY_INDEX_2("fileType", "S");

    public String key;
    public String type;

    TableDescription(String key, String type) {
        this.key = key;
        this.type = type;
    }
}
