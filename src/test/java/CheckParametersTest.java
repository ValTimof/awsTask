import dynamoDB.MyTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.AWSService;
import tools.Config;

import static enums.StringConstants.AWS_SOURCE_ARN;
import static enums.StringConstants.FUNCTION_ARN;
import static org.junit.jupiter.api.Assertions.*;


public class CheckParametersTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService(config.BUCKET_NAME, config.LAMBDA_NAME, config.TABLE_NAME);

    @Test
    @DisplayName("Checks parameters in s3, lambda and dynamodb")
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
