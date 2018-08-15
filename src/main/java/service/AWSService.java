package service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
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
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.jayway.jsonpath.JsonPath;
import dynamoDB.MyTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;

public class AWSService {

    private AmazonS3 s3Client;
    private AWSLambda lambdaClient;
    private AmazonDynamoDB dynamoDBClient;
    private Table table;
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
        this.table = new DynamoDB(dynamoDBClient).getTable(tableName);
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

    public String getBucketNotificationParameter(String parameterName) {
        String notificationConfiguration = s3Client.getBucketNotificationConfiguration(bucketName).toString();
        List<String> parameterValue = JsonPath.read(notificationConfiguration, "$.." + parameterName);
        return parameterValue.toString();
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
        String roleName = roleArn.substring(roleArn.indexOf('/') + 1);
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

    public String uploadFileToBucket(String key, File file) {
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
        return key;
    }

    public void deleteObjectFromBucket(String objectKey) {
        try {
            s3Client.getObject(bucketName, objectKey);
            s3Client.deleteObject(bucketName, objectKey);
        } catch (AmazonS3Exception e) {
            System.err.println(e.getErrorMessage());
        } catch (AmazonServiceException e) {
            System.out.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    private ArrayList<PrimaryKey> getItemPrimaryKeys(String s3objectKey) {
        ArrayList<PrimaryKey> primaryKeys = new ArrayList<>();
        try {
            ScanExpressionSpec scanSpec = new ExpressionSpecBuilder().withCondition(
                    S("filePath").eq(s3objectKey))
                    .buildForScan();

            ItemCollection<ScanOutcome> items = table.scan(scanSpec);
            for (Item item : items) {
                primaryKeys.add(new PrimaryKey(
                        "packageId", item.getString("packageId"),
                        "originTimeStamp", item.getLong("originTimeStamp")));
            }
        } catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }
        return primaryKeys;
    }

    private ArrayList<PrimaryKey> getItemPrimaryKeys(String actionName, String s3objectKey) {
        ArrayList<PrimaryKey> primaryKeys = new ArrayList<>();
        try {
            ScanExpressionSpec scanSpec = new ExpressionSpecBuilder().withCondition(
                    S("filePath").eq(s3objectKey).and(S("actionName").eq(actionName)))
                    .buildForScan();

            ItemCollection<ScanOutcome> items = table.scan(scanSpec);
            for (Item item : items) {
                primaryKeys.add(new PrimaryKey(
                        "packageId", item.getString("packageId"),
                        "originTimeStamp", item.getLong("originTimeStamp")));
            }
        } catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }
        return primaryKeys;
    }

    private void deleteItemFromDB(String s3objectKey) {
        for (PrimaryKey itemPrimaryKey : getItemPrimaryKeys(s3objectKey)) {
            DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                    .withPrimaryKey(itemPrimaryKey);
            table.deleteItem(deleteItemSpec);
            System.out.printf("The item %s deleted from dynamo DB table\n", itemPrimaryKey.getComponents().toString());
        }
    }

    public Callable<Boolean> newItemIsAdded(String s3objectKey) {
        return () -> {
            return getItemPrimaryKeys(s3objectKey).size() == 1; // The condition that must be fulfilled
        };
    }

    public boolean lambdaWasTriggered(String s3objectKey) {
        return getItemPrimaryKeys(s3objectKey).size() > 0;
    }

    public boolean lambdaWasTriggered(String actionName, String s3objectKey) {
        return getItemPrimaryKeys(actionName, s3objectKey).size() > 0;
    }

    public void cleanS3Bucket(String s3objectKey) {
        try {
            //delete object from S3
            deleteObjectFromBucket(s3objectKey);
        } catch (AmazonServiceException e) {
            System.err.println("The call was transmitted successfully, but Amazon S3 couldn't process");
            System.err.println(e.getMessage());
        } catch (SdkClientException e) {
            System.err.println("Amazon S3 couldn't be contacted for a response");
            System.err.println(e.getMessage());
        }
    }

    public void cleanDataBase(String s3objectKey) {
        try {
            //delete item from dynamoDB
            deleteItemFromDB(s3objectKey);
        } catch (AmazonServiceException e) {
            System.err.println("The call was transmitted successfully, but Amazon S3 couldn't process");
            System.err.println(e.getMessage());
        } catch (SdkClientException e) {
            System.err.println("Amazon S3 couldn't be contacted for a response");
            System.err.println(e.getMessage());
        }
    }
}
