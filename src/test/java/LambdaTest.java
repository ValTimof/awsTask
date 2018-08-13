import dynamoDB.MyTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import service.AWSService;

import java.io.File;
import java.io.IOException;

import static enums.StringConstants.TEST_PREFIX;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.FileHandler.createSampleFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LambdaTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService(config.BUCKET_NAME, config.TABLE_NAME, config.LAMBDA_NAME);

    @Test
    void whenUploadFileToS3_thenLambdaWasTriggered() throws IOException, InterruptedException {
        assertAll(
                () -> assertTrue(service.doesBucketExist(), "The bucket can't be found."),
                () -> assertTrue(service.doesLambdaExist(), "The lambda can't be found."),
                () -> assertTrue(service.doesDBTableExist(), "The table can't be found.")
        );

        File file = createSampleFile();
        service.uploadFileToBucket(TEST_PREFIX.key + file.getName(), file);
//        awaitUntil(); много ассертов
        Thread.sleep(2000);
        assertTrue(service.lambdaWasTriggeredOnUpload(), "The lambda not triggered");

        service.cleanUp();
    }

    @Test
    void whenUploadFileToS3_andRemoveUploadedFileFromS3_thenLambdaWasTriggered() {
        assertAll(
                () -> assertTrue(service.doesBucketExist(), "The bucket can't be found."),
                () -> assertTrue(service.doesLambdaExist(), "The lambda can't be found."),
                () -> assertTrue(service.doesDBTableExist(), "The table can't be found.")
        );
    }
}
