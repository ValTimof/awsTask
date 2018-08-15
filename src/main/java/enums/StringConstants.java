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
    TEST_PREFIX("test/"),
    OBJECT_CREATED("ObjectCreated:Put"),
    OBJECT_REMOVED("ObjectRemoved:Delete");

    public String key;


    StringConstants(String key) {
        this.key = key;
    }
}
