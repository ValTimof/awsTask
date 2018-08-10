import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import s3.AWSService;

import java.util.List;
import java.util.ResourceBundle;

import static enums.StringConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CheckParametersTest {
    private static final String FUNCTION_ARN = "functionARN";
    private static final String AWS_SOURCE_ARN = "AWS:SourceArn";
    private static final String RUNTIME_VALUE = "java8";
    private static final String HANDLER_VALUE = "com.ebsco.platform.infrastructure.inventoringlambda.IngestionLambda";

    private AWSService service;
    private String bucketName;
    private String lambdaName;
    private String tableName;


    @BeforeAll
    void setUp() {
        service = new AWSService();
        bucketName = ResourceBundle.getBundle(PROPERTIES_FILE.text).getString(BUCKET.text);
        lambdaName = ResourceBundle.getBundle(PROPERTIES_FILE.text).getString(LAMBDA.text);
        tableName = ResourceBundle.getBundle(PROPERTIES_FILE.text).getString(TABLE.text);
    }

    @Test
    void checksParametersInS3LambdaAndDynamoDB() {
        assertTrue(service.isBucketExist(bucketName));
        assertThat(service.getBucketNotificationParameter(bucketName, FUNCTION_ARN)).contains(lambdaName);
        assertThat(service.getLambdaPolicyParameter(lambdaName, AWS_SOURCE_ARN)).contains(bucketName);
        assertThat(service.getLambdaRuntimeConfiguration(lambdaName)).isEqualTo(RUNTIME_VALUE);
        assertThat(service.getLambdaHandlerConfiguration(lambdaName)).isEqualTo(HANDLER_VALUE);

        ScanExpressionSpec xspec = new ExpressionSpecBuilder().buildForScan();
        Table table = new DynamoDB(service.dynamoDBClient).getTable(tableName);
        TableDescription tableInfo = table.describe();


        List<AttributeDefinition> attributes =
                tableInfo.getAttributeDefinitions();
        System.out.println("Attributes");
        for (AttributeDefinition a : attributes) {
            System.out.format("  %s (%s)\n",
                    a.getAttributeName(), a.getAttributeType());
        }
    }
}
