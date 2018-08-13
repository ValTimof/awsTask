package service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.GetPolicyRequest;
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

    private AmazonS3 s3Client;
    public AWSLambda lambdaClient;
    private AmazonDynamoDB dynamoDBClient;
    private AmazonIdentityManagement iam;

    private String bucketName;
    private String lambdaName;
    private String tableName;

    public AWSService(String bucketName, String lambdaName, String tableName) {
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
        this.dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.lambdaClient = AWSLambdaClientBuilder.defaultClient();
        this.iam = AmazonIdentityManagementClientBuilder.defaultClient();

        this.bucketName = bucketName;
        this.lambdaName = lambdaName;
        this.tableName = tableName;
    }

    public boolean doesBucketExist() {
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket bucket : buckets) {
            if (bucket.getName().equals(bucketName)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesLambdaExist() {
        List<FunctionConfiguration> functions = lambdaClient.listFunctions().getFunctions();
        for (FunctionConfiguration function : functions) {
            if (function.getFunctionName().equals(lambdaName)) {
                return true;
            }
        }
        return false;
    }

    public boolean doesDBTableExist() {
        List<String> tableNames = dynamoDBClient.listTables().getTableNames();
        for (String tName : tableNames) {
            if (tName.equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    public void uploadFileToBucket(String key, File file) {
        TransferManager transferManager = TransferManagerBuilder.defaultTransferManager();
        try {
            Upload myUpload = transferManager.upload(bucketName, key, file);
            myUpload.waitForCompletion();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Transfer interrupted: " + e.getMessage());
            System.exit(1);
        }
    }

    public String getBucketNotificationParameter(String parameterName) {
        String notificationConfiguration = s3Client.getBucketNotificationConfiguration(bucketName).toString();
        List<String> parameterValue = JsonPath.read(notificationConfiguration, "$.." + parameterName);
        return parameterValue.toString();
    }

    public void deleteObjectFromBucket(String objectKey) {
        try {
            s3Client.deleteObject(bucketName, objectKey);
        } catch (AmazonServiceException e) {
            System.out.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    public String getLambdaPolicyParameter(String parameterName) {
        String policy = lambdaClient.getPolicy(new GetPolicyRequest()
                .withFunctionName(lambdaName)).getPolicy();
        List<String> parameterValue = JsonPath.read(policy, "$.." + parameterName);
        return parameterValue.toString();
    }

    public String getLambdaRuntimeConfiguration() {
        return lambdaClient.getFunctionConfiguration(new GetFunctionConfigurationRequest()
                .withFunctionName(lambdaName)).getRuntime();
    }

    public String getLambdaHandlerConfiguration() {
        return lambdaClient.getFunctionConfiguration(new GetFunctionConfigurationRequest()
                .withFunctionName(lambdaName)).getHandler();
    }

    public boolean checkLambdaIAMPilicies(String policyName) {
        String roleArn = lambdaClient.getFunctionConfiguration(new GetFunctionConfigurationRequest()
                .withFunctionName(lambdaName)).getRole();
        String roleName = roleArn.substring(roleArn.indexOf('/')+1, roleArn.length());
        String rolePolicy = "";
        try {
            GetRolePolicyResult rolePolicyResult = iam.getRolePolicy(new GetRolePolicyRequest()
                    .withPolicyName(policyName).withRoleName(roleName));
            rolePolicy = rolePolicyResult.getRoleName();
        } catch (NoSuchEntityException e) {
            System.err.println(e.getMessage());
        }
        return !rolePolicy.equals("");
    }

    public boolean checkDBTable(MyTable expectedTable) {
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

    public void putItemToDataBase(Map<String, AttributeValue> itemValues) {
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

    public boolean lambdaWasTriggeredOnUpload() {
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
