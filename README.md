A custom-built log4j appender that sends over log messages to a SQS queue
================================
# Introduction
From time to time, we want to monitor a remote application through its log messages in near realtime. For example, we could tunnel these log messages into Splunk and set up alerts monitor exceptions and warnings.
`AWS Cloudwatch` is such a system, but comes with its limitations and specific requirements. This tiny little log4j appender library will allow you to set up a similar remote log monitor system with minimum cost (SQS is super cheap) with your JAVA application. For python application, check out (the python equivalent here)[https://github.com/zillow/python-sqs-logging-handler]

# Case study
We use this log4j appender and its (python equivalent)[https://github.com/zillow/python-sqs-logging-handler] to monitor our EMR Spark applications. Getting near-realtime log messages from Spark/Hadoop is difficult because log output from the executors are written to HDFS, and is only collectable when the application finishes. Only log outputs from the master node and other global status messages are tracked in realtime by `CloudWatch`. To solve this problem, we simply rolled up our own log4j appender. For more details, see usage section.

# Scale of your application
AWS SQS is NOT for large-scale message processing. You can use `AWS Kinesis` stream for large scale log messages, and pay a premium.

# Usage
To use this log appender, make sure:

**1. The appender jar is copied to CLASSPATH of the Java application.**

For EMR users, use bootstrap action to download this jar from s3 to all the nodes of your cluster. Make sure the jar is copied to HADOOP_CLASSPATH. 
Check /etc/hadoop/conf/hadoop-env.sh to view environments.

Example bootstrap stript:
```
sudo aws --region us-west-2 s3 cp s3://some/s3/bucket/sqs-log4j-handler.jar /usr/share/aws/emr/emrfs/lib/sqs-log4j-handler.jar
```

**2. Update log4j configuration file.**

Check [Configuring Applications](http://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-configure-apps.html) to learn how to override default configurations for hadoop-log4j or spark-log4j on EMR master node.

For Spark/hadoop, the jar needs to be copied to all master/slave nodes, while 
the configuration file only needs to be updated on the master. Here is an example:

    log4j.rootCategory=INFO,console,sqs
    
    # Existing appender that writes to console
    log4j.appender.console=org.apache.log4j.ConsoleAppender
    log4j.appender.console.target=System.err
    log4j.appender.console.layout=org.apache.log4j.PatternLayout
    log4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n
    
    # New appender that sends log message to a queue
    log4j.appender.sqs=com.zillow.zda.SqsAppender
    
    # Custom layout that converts log message into Zillow Splunk
    # Json data format
    log4j.appender.sqs.layout=com.zillow.zda.SplunkLayout
    
    # (Optional) set the maximum size of the json message, so it
    # does not crash downstream consumers. For example, SQS may 
    # have a limit on the message size it can receive. This limit,
    # however, should also be larger enough (> 1000 characters) to
    # allow sensible log messages containing enough information.
    log4j.appender.sqs.layout.MaxMessageSize=50000
    
    # AWS queue name
    log4j.appender.sqs.QueueName=sqs-queue-name

    # Provide AWS credentials to access the queue.
    # Ignore this part if using assumed IAM roles.
    log4j.appender.sqs.AccessKey=your_access_key_id
    log4j.appender.sqs.SecretKey=aws_access_key
    
    # (Optional) Set the cluster name, so each log entry will be marked with
    # the spark cluster name
    log4j.appender.sqs.ClusterName=regression

    # (Optional) Some loggers in spark is very verbose
    org.apache.spark.deploy.yarn.YarnAllocator=WARN
    org.apache.spark.executor.CoarseGrainedExecutorBackend=WARN
    org.apache.spark.executor.Executor=WARN
    org.apache.spark.scheduler.TaskSetManager=WARN
    org.apache.spark.sql.execution.datasources.FileScanRDD=WARN
    
    # (Optional) This guy is also very verbose
    com.amazon.ws.emr.hadoop.fs.s3n.S3NativeFileSystem=WARN
    

