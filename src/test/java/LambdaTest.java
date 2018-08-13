import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import dynamoDB.MyTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import service.AWSService;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static enums.StringConstants.TEST_PREFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.FileHandler.createSampleFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LambdaTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService();

    @Test
    void givenBucketExist_whenUploadFileToS3_thenLambdaWasTriggered() throws IOException, InterruptedException {
        assertTrue(service.doesBucketExist(config.BUCKET_NAME));
        assertTrue(service.doesLambdaExist(config.LAMBDA_NAME));
        assertTrue(service.checkDBTable(config.TABLE_NAME, myTable));

        File file = createSampleFile();
        service.uploadFileToBucket(config.BUCKET_NAME, TEST_PREFIX.key + file.getName(), file);
        Thread.sleep(2000);
        assertTrue(service.lambdaWasTriggered(config.TABLE_NAME));

        service.cleanUp();


    }

    @Test
    void whenUploadFileToS3_andRemoveUploadedFileFromS3_thenLambdaWasTriggered() {

    }
}
