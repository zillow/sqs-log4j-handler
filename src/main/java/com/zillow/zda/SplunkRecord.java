package com.zillow.zda;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Created by weil1 on 12/22/16.
 * <p>
 * Zillow splunk uses an uniformed format for log data.
 * Convert log data into this uniformed format helps ops team to interpret the logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SplunkRecord {
    /* Truncate the stack trace so we do not send an exceedingly large message to SQS*/
    private static int MAX_STACK_TRACE_LENGTH = 10000;

    /*
        Splunk timestamp is in the format of 2016-02-13T12:03:30.542UTC, i.e., python format.
        However, Java does not allow inserting arbitrary character 'T' in the middle of the format
        string. As a result, we have to print the date and time parts separately, and join them with
        the letter 'T' later.
    */
    private static DateTimeFormatter FORMAT1 = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static DateTimeFormatter FORMAT2 = DateTimeFormat.forPattern("HH:mm:ss.Szzz");

    private static String hostName;
    private static String clusterName;

    /* time stamp in UTC */
    private String tsi;
    /* log message */
    private Object msg;
    /* log level */
    private String lvl;
    /* location information (class, file, line no of the log source */
    private String cls;
    private String method;
    private String file;
    private String lineno;
    /* logger name */
    private String name;

    private Map<String, String> properties;

    private String exception;

    /*
    private String exception;

    /**
     * Convert a log event into Splunk record.
     * @param event
     *      log event
     */
    public SplunkRecord(LoggingEvent event) {
        DateTime date = new DateTime(event.timeStamp);

        // See comment on format variables on why we need to use
        // two separate formats.
        this.tsi = FORMAT1.print(date) + "T" + FORMAT2.print(date);

        this.msg = event.getMessage();
        this.lvl = event.getLevel().toString();
        LocationInfo locationInfo = event.getLocationInformation();
        this.cls = locationInfo.getClassName();
        this.method = locationInfo.getMethodName();
        this.file = locationInfo.getFileName();
        this.lineno = locationInfo.getLineNumber();
        this.properties = event.getProperties();
        this.name = event.getLoggerName();

        // Set to null so the serializer can ignore it
        if(this.properties.keySet().size() == 0) {
            this.properties = null;
        }

        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if(throwableInformation != null) {
            Throwable throwable = throwableInformation.getThrowable();
            if (throwable != null) {
                StringWriter writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                throwable.printStackTrace(printWriter);
                printWriter.flush();

                this.exception = writer.toString();
                if (this.exception.length() > MAX_STACK_TRACE_LENGTH) {
                    this.exception = "Truncated stack trace:" + this.exception.substring(0, MAX_STACK_TRACE_LENGTH);
                }
            }
        }
    }

    /**
     * Sets the cluster name. Which will be recorded for every log entry.
     * Only needs to set it once per JVM process.
     * @param clusterName
     */
    static void setClusterName(String clusterName) {
        SplunkRecord.clusterName = clusterName.trim();
    }

    @JsonProperty(value ="tsi", index = 0)
    public String getTsi() {
        return this.tsi;
    }

    @JsonProperty(value ="lvl", index = 1)
    public String getLvl() {
        return this.lvl;
    }

    @JsonProperty(value="msg", index = 2)
    public Object getMsg() {
        return this.msg;
    }

    @JsonProperty(value="name", index = 8)
    public String getName() { return this.name; }

    @JsonProperty(value="cls", index = 14)
    public Object getCls() {
        return this.cls;
    }

    @JsonProperty(value="method", index = 20)
    public Object getMethod() {
        return this.method;
    }

    @JsonProperty(value="file", index = 26)
    public Object getFile() {
        return this.file;
    }

    @JsonProperty(value="lineno", index = 30)
    public Object getLineNo() {
        return this.lineno;
    }

    @JsonProperty(value="properties", index = 35)
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @JsonProperty(value="exception", index = 40)
    public String getException() {
        return this.exception;
    }

    @JsonProperty(value="host", index = 45)
    public String getHostName() {
        if (SplunkRecord.hostName == null) {
            try {
                SplunkRecord.hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                throw new RuntimeException("Unable to get the host name:", ex);
            }
        }

        return SplunkRecord.hostName;
    }

    @JsonProperty(value="cluster", index = 50)
    public String getClusterName() { return SplunkRecord.clusterName; }


}
