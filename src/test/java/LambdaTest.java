import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import s3.AWSService;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import static enums.StringConstants.BUCKET;
import static enums.StringConstants.PROPERTIES_FILE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tools.FileHandler.createSampleFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LambdaTest {
    private static final String TEST_PREFIX = "test/";

    private AWSService service;
    private String bucketName;


    @BeforeAll
    void setUp() {
        service = new AWSService();
        bucketName = ResourceBundle.getBundle(PROPERTIES_FILE.text).getString(BUCKET.text);
    }

    @Test
    void givenBucketExist_whenUploadFileToS3_thenLambdaWasTriggered() throws IOException {
        assertTrue(service.isBucketExist(bucketName));
        File file = createSampleFile();
        service.uploadFileToBucket(bucketName, TEST_PREFIX + file.getName(), file);

    }

    @Test
    void whenUploadFileToS3_andRemoveUploadedFileFromS3_thenLambdaWasTriggered() {

    }
}
