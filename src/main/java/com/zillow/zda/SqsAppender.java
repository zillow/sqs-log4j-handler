package com.zillow.zda;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Created by weil1 on 12/20/16.
 */
public class SqsAppender extends AppenderSkeleton {
    private static final Logger LOGGER = Logger.getLogger(SqsAppender.class);
    private String region = Regions.US_WEST_2.getName();
    private String queueName;
    private String awsAccessKey = "";
    private String awsSecretKey = "";
    private String clusterName = "";
    private AmazonSQSBufferedAsyncClient client;
    private String queueUrl;
    private boolean initializationFailed = false;

    private void error(String message) {
        error(message, null);
    }

    private void error(String message, Exception e) {
        LOGGER.error(message, e);
        errorHandler.error(message, e, ErrorCode.GENERIC_FAILURE);
        throw new IllegalStateException(message, e);
    }

    @Override
    public void append(LoggingEvent logEvent) {
        if (initializationFailed) {
            error("Unable to log event due to failed appender initialization of " + name);
            return;
        }

        try {
            String message = layout.format(logEvent);
            SendMessageRequest request = new SendMessageRequest(this.queueUrl, message);
            this.client.sendMessageAsync(request);
        } catch (Exception e) {
            // Swallow the exception here instead of logging it because logging the exception will cause yet another call to SQS
            // log appender, leading to infinite loop when SQS service is 
            // down or unstable.
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    @Override
    public void activateOptions() {
        AmazonSQSAsync asyncClient;
        try {
            AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
            if (awsAccessKey != "" && awsSecretKey != "") {
                AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
                asyncClient = clientBuilder.withCredentials(credentialsProvider)
                                           .build();
            } else {
                LOGGER.info("Either access key id or the secret key is not provided. Fall back to default aws credentials");
                asyncClient = clientBuilder.build();
            }
            this.client = new AmazonSQSBufferedAsyncClient(asyncClient);
            this.client.setRegion(Region.getRegion(Regions.fromName(region)));
            this.queueUrl = this.client.getQueueUrl(this.queueName).getQueueUrl();
        } catch (Exception ex) {
            initializationFailed = true;
            error("Unable to initialize SQS service with queue name:" + this.queueName, ex);
        }
    }

    /**
     * Returns configured queue name
     *
     * @return configured queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Sets queueName for the SQS queue to which the log data is sent
     *
     * @param queueName name of the SQS queue to which the log data is sent
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName.trim();
    }

    /**
     * Gets the AWS access key (key id) used for writing to the SQS queue
     */
    public String getAccessKey() {
        return this.awsAccessKey;
    }

    /**
     * Sets the AWS access key (key id) used for writing to the SQS queue
     */
    public void setAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey.trim();
    }

    /**
     * Gets the secret AWS access key used for writing to the SQS queue
     */
    public String getSecretKey() {
        return this.awsSecretKey;
    }

    /**
     * Sets the secrete AWS access key used for writing to the SQS queue
     */
    public void setSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey.trim();
    }

    /**
     * Returns configured cluster name
     *
     * @return configured cluster name
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * (Optional) sets the cluster name
     *
     * @param clusterName name of the Spark cluster
     */
    public void setClusterName(String clusterName) {

        this.clusterName = clusterName.trim();
        SplunkRecord.setClusterName(this.clusterName);
    }
}
