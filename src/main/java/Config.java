import java.util.ResourceBundle;

import static enums.StringConstants.*;

public class Config {
    public static final String PROPERTIES_FILE_NAME = CONFIG.key;

    public final String BUCKET_NAME;
    public final String LAMBDA_NAME;
    public final String TABLE_NAME;
    public final String RUNTIME_NAME;
    public final String HANDLER_NAME;

    public Config() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(PROPERTIES_FILE_NAME);
        BUCKET_NAME = resourceBundle.getString(BUCKET.key);
        LAMBDA_NAME = resourceBundle.getString(LAMBDA.key);
        TABLE_NAME = resourceBundle.getString(TABLE.key);
        RUNTIME_NAME = resourceBundle.getString(RUNTIME.key);
        HANDLER_NAME = resourceBundle.getString(HANDLER.key);
    }


}
