import dynamoDB.MyTable;
import org.junit.jupiter.api.Test;
import service.AWSService;

import java.io.File;
import java.io.IOException;

import static enums.StringConstants.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.FileHandler.createSampleFile;

public class LambdaTest {

    private final Config config = new Config();
    private final MyTable myTable = new MyTable();
    private final AWSService service = new AWSService(config.BUCKET_NAME, config.LAMBDA_NAME, config.TABLE_NAME);

    @Test
    void whenUploadFileToS3_thenLambdaWasTriggered() throws IOException, InterruptedException {
        assertAll(
                () -> assertTrue(service.doesBucketExist(), "The bucket can't be found."),
                () -> assertTrue(service.doesLambdaExist(), "The lambda can't be found."),
                () -> assertTrue(service.doesDBTableExist(), "The table can't be found.")
        );
        File file = createSampleFile();
        String objectKey = service.uploadFileToBucket(TEST_PREFIX.key + file.getName(), file);

        await().until(service.newItemIsAdded(objectKey));
        assertTrue(service.lambdaWasTriggered(OBJECT_CREATED.key, objectKey), "The lambda not triggered for upload");

        service.cleanS3Bucket(objectKey);
        await().until(service.newItemIsAdded(objectKey));
        service.cleanDataBase(objectKey);
    }

    @Test
    void whenUploadFileToS3_andRemoveUploadedFileFromS3_thenLambdaWasTriggered() throws IOException, InterruptedException {
        assertAll(
                () -> assertTrue(service.doesBucketExist(), "The bucket can't be found."),
                () -> assertTrue(service.doesLambdaExist(), "The lambda can't be found."),
                () -> assertTrue(service.doesDBTableExist(), "The table can't be found.")
        );
        File file = createSampleFile();
        String objectKey = service.uploadFileToBucket(TEST_PREFIX.key + file.getName(), file);
        service.deleteObjectFromBucket(objectKey);

        await().until(service.newItemIsAdded(objectKey));
        assertTrue(service.lambdaWasTriggered(OBJECT_CREATED.key, objectKey), "The lambda not triggered for upload");
        assertTrue(service.lambdaWasTriggered(OBJECT_REMOVED.key, objectKey), "The lambda not triggered for remove");

        service.cleanDataBase(objectKey);
    }
}
