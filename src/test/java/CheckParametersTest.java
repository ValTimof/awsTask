import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import dynamoDB.MyTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import service.AWSService;

import java.util.List;

import static enums.StringConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CheckParametersTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService();

    @Test
    void checksParametersInS3LambdaAndDynamoDB() {
        assertTrue(service.doesBucketExist(config.BUCKET_NAME));
        assertTrue(service.doesLambdaExist(config.LAMBDA_NAME));
        assertTrue(service.checkDBTable(config.TABLE_NAME, myTable));
        assertThat(service.getBucketNotificationParameter(config.BUCKET_NAME, FUNCTION_ARN.key)).contains(config.LAMBDA_NAME);
        assertThat(service.getLambdaPolicyParameter(config.LAMBDA_NAME, AWS_SOURCE_ARN.key)).contains(config.BUCKET_NAME);
        assertThat(service.getLambdaRuntimeConfiguration(config.LAMBDA_NAME)).isEqualTo(config.RUNTIME_NAME);
        assertThat(service.getLambdaHandlerConfiguration(config.LAMBDA_NAME)).isEqualTo(config.HANDLER_NAME);
    }
}
