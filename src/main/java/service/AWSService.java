package service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetPolicyRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.jayway.jsonpath.JsonPath;
import dynamoDB.MyTable;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static enums.StringConstants.TEST_PREFIX;

public class AWSService {

    public final AmazonS3 s3Client;
    private final AWSLambda lambdaClient;
    public final AmazonDynamoDB dynamoDBClient;

    public AWSService() {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.lambdaClient = AWSLambdaClientBuilder.defaultClient();
    }

    public void uploadFileToBucket(String bucketName, String key, File file) {
        TransferManager transferManager = TransferManagerBuilder.defaultTransferManager();
        try {
            Upload transfer = transferManager.upload(bucketName, key, file);
            transfer.waitForCompletion();
//            XferMgrProgress.showTransferProgress(transfer);
//            XferMgrProgress.waitForCompletion(transfer);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Transfer interrupted: " + e.getMessage());
            System.exit(1);
    }
    }

    public boolean doesBucketExist(String bucketName) {
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket.getName().equals(bucketName)) {
                return true;
            }
        }
        return false;
    }

    public void deleteObjectFromBucket(String bucketName, String objectKey) {
        try {
            s3Client.deleteObject(bucketName, objectKey);
        } catch (AmazonServiceException e) {
            System.out.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    public boolean doesLambdaExist(String lambdaName) {
        List<FunctionConfiguration> functions = lambdaClient.listFunctions().getFunctions();
        for (FunctionConfiguration function : functions) {
            if (function.getFunctionName().equals(lambdaName)) {
                return true;
            }
        }
        return false;
    }

    public String getLambdaPolicyParameter(String lambdaName, String parameterName) {
        String policy = lambdaClient.getPolicy(new GetPolicyRequest()
                .withFunctionName(lambdaName)).getPolicy();
        List<String> parameterValue = JsonPath.read(policy, "$.." + parameterName);
        return parameterValue.toString();
    }

    public String getBucketNotificationParameter(String bucketName, String parameterName) {
        String notificationConfiguration = s3Client.getBucketNotificationConfiguration(bucketName).toString();
        List<String> parameterValue = JsonPath.read(notificationConfiguration, "$.." + parameterName);
        return parameterValue.toString();
    }

    public String getLambdaRuntimeConfiguration(String lambdaName) {
        return lambdaClient.getFunctionConfiguration(new GetFunctionConfigurationRequest()
                .withFunctionName(lambdaName)).getRuntime();
    }

    public String getLambdaHandlerConfiguration(String lambdaName) {
        return lambdaClient.getFunctionConfiguration(new GetFunctionConfigurationRequest()
                .withFunctionName(lambdaName)).getHandler();
    }

    public boolean checkDBTable(String tableName, MyTable expectedTable) {
        Table table = new DynamoDB(dynamoDBClient).getTable(tableName);
        TableDescription tableInfo = table.describe();
        List<AttributeDefinition> attributes =
                tableInfo.getAttributeDefinitions();
        for (AttributeDefinition a : attributes) {
            if (!a.getAttributeName().equals(expectedTable.attributes.get(attributes.indexOf(a)).attributeName)
                && !a.getAttributeType().equals(expectedTable.attributes.get(attributes.indexOf(a)).attributeType)) {
                return false;
            }
        }
        return true;
    }

    public void putItemToDataBase(String tableName, Map<String, AttributeValue> itemValues) {
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

    public boolean lambdaWasTriggered(String tableName) {
        DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);
        Table table = dynamoDB.getTable(tableName);

        ScanExpressionSpec scanSpec = new ExpressionSpecBuilder().withCondition(
                S("filePath").beginsWith(TEST_PREFIX.key))
                .buildForScan();

        try {
            ItemCollection items = table.scan(scanSpec);
            for (Object item : items) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }
        return false;
    }

    public void cleanUp() {
    }
}
