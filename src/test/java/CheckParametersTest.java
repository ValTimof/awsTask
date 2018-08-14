import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import dynamoDB.MyTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import service.AWSService;

import java.util.List;

import static enums.StringConstants.AWS_SOURCE_ARN;
import static enums.StringConstants.FUNCTION_ARN;
import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CheckParametersTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService(config.BUCKET_NAME, config.LAMBDA_NAME, config.TABLE_NAME);

    @Test
    void checksParametersInS3LambdaAndDynamoDB() {
        assertAll(
                () -> assertTrue(service.doesBucketExist(), "The bucket can't be found."),
                () -> assertTrue(service.doesLambdaExist(), "The lambda can't be found."),
                () -> assertTrue(service.doesDBTableExist(), "The table can't be found.")
        );
        assertAll(
                () -> assertTrue(service.checkDBTable(myTable), "The table has wrong format."),
                () -> assertTrue(service.getBucketNotificationParameter(FUNCTION_ARN.key).contains(config.LAMBDA_NAME)),
                () -> assertTrue(service.getLambdaPolicyParameter(AWS_SOURCE_ARN.key).contains(config.BUCKET_NAME)),
                () -> assertTrue(service.getLambdaRuntimeConfiguration().contains(config.RUNTIME_NAME)),
                () -> assertEquals(config.HANDLER_NAME, service.getLambdaHandlerConfiguration())
        );
    }
}
