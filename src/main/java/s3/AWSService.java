package s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.jayway.jsonpath.JsonPath;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AWSService {

    private final AmazonS3 s3Client;
    public final AmazonDynamoDB dynamoDBClient;
    private final AWSLambda lambdaClient;

    public AWSService() {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.lambdaClient = AWSLambdaClientBuilder.defaultClient();
    }

    public void uploadFileToBucket(String bucketName, String key, File file) {
        TransferManager transferManager = TransferManagerBuilder.defaultTransferManager();
        try {
            Upload transfer = transferManager.upload(bucketName, key, file);
            XferMgrProgress.showTransferProgress(transfer);
            XferMgrProgress.waitForCompletion(transfer);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    public boolean isBucketExist(String bucketName) {
        List<Bucket> buckets = s3Client.listBuckets();
        boolean flag = false;
        for (Bucket bucket : buckets) {
            if (bucket.getName().equals(bucketName)) {
                flag = true;
            }
        }
        return flag;
    }

    public void deleteObjectFromBucket(String bucketName, String objectKey) {
        try {
            s3Client.deleteObject(bucketName, objectKey);
        } catch (AmazonServiceException e) {
            System.out.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    public void putItemToDataBase(String tableName, Map<String,AttributeValue> itemValues) {
        try {
            dynamoDBClient.putItem(tableName, itemValues);
        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The table \"%s\" can't be found.\n", tableName);
            System.err.println("Be sure that it exists and that you've typed its text correctly!");
            System.exit(1);
        } catch (AmazonServiceException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Done!");
    }

    public String getLambdaPolicyParameter(String lambdaName, String parameterName) {
        GetPolicyRequest getPolicyRequest = new GetPolicyRequest();
        getPolicyRequest.setFunctionName(lambdaName);

        String policy = lambdaClient.getPolicy(getPolicyRequest).getPolicy();
        List<String> parameterValue = JsonPath.read(policy, "$.."+parameterName);
        return parameterValue.toString();
    }

    public String getBucketNotificationParameter(String bucketName, String parameterName) {
        String notificationConfiguration = s3Client.getBucketNotificationConfiguration(bucketName).toString();
        List<String> parameterValue = JsonPath.read(notificationConfiguration, "$.."+parameterName);
        return parameterValue.toString();
    }

    public String getLambdaRuntimeConfiguration(String lambdaName) {
        GetFunctionConfigurationRequest getFonConf = new GetFunctionConfigurationRequest();
        getFonConf.setFunctionName(lambdaName);
        return lambdaClient.getFunctionConfiguration(getFonConf).getRuntime();
    }

    public String getLambdaHandlerConfiguration(String lambdaName) {
        GetFunctionConfigurationRequest getFonConf = new GetFunctionConfigurationRequest();
        getFonConf.setFunctionName(lambdaName);
        return lambdaClient.getFunctionConfiguration(getFonConf).getHandler();
    }
}