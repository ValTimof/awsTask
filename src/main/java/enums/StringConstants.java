package enums;

public enum StringConstants {
    PROPERTIES_FILE("config"), BUCKET("bucketName"),  LAMBDA("lambdaName"), TABLE("tableName");

    public String text;

    StringConstants(String title) {
        this.text = title;
    }
}
