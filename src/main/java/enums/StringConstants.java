package enums;

public enum StringConstants {
    CONFIG("config"),
    BUCKET("bucketName"),
    LAMBDA("lambdaName"),
    TABLE("tableName"),
    FUNCTION_ARN("functionARN"),
    AWS_SOURCE_ARN("AWS:SourceArn"),
    RUNTIME("runtime"),
    HANDLER("handlerName"),
    TEST_PREFIX("test/");

    public final String key;

    StringConstants(String title) {
        this.key = title;
    }
}
