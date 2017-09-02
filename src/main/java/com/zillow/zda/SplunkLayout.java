package com.zillow.zda;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Created by weil1 on 12/22/16.
 */
public class SplunkLayout extends Layout {

    /*
     When setting the message size limit, it should not be too low.
    */
    private static int MINIMUM_MESSAGE_SIZE_LIMIT = 1000;

    /*
     Limit the message size for downstream consumption.
     Message exceeding this limit will be replaced with a smaller warning message.
    */
    private int maxMessageSize = 150000;

    ObjectMapper mapper = new ObjectMapper();

    private boolean invalidConfiguration = false;

    @Override
    public String format(LoggingEvent loggingEvent) {
        if(invalidConfiguration) {
            throw new RuntimeException("configuration is invalid, check if the message size limit is properly set");
        }

        SplunkRecord record = new SplunkRecord(loggingEvent);
        String json = createJsonText(record);

        if(json.length() > this.maxMessageSize) {
            String message = String.format("Log message size: %s exceeding limit: %s", json.length(), this.maxMessageSize);

            // Keep the log source information, but throw away everything else.
            LoggingEvent replacement = new LoggingEvent(loggingEvent.fqnOfCategoryClass, loggingEvent.getLogger(), loggingEvent.timeStamp, loggingEvent.getLevel(), message, null);
            json = createJsonText(new SplunkRecord(replacement));
        }
        return json;
    }

    private String createJsonText(SplunkRecord record) {
        String json = "";
        try {
            json = mapper.writeValueAsString(record);
        }
        catch(Exception ex) {
            throw new RuntimeException("Unable to convert splunk record to JSON text", ex);
        }
        return json;
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public void activateOptions() {
        if(maxMessageSize < MINIMUM_MESSAGE_SIZE_LIMIT) {
            this.invalidConfiguration = true;
            throw new RuntimeException("the message size limit shouldn't be lower than" + MINIMUM_MESSAGE_SIZE_LIMIT);
        }

        // The object mapper is set to be accepting here, because the client
        // may not set its log message object to be public.
        this.mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Sets the maximum limit of the json text produced by the formatter.
     * Message exceeding this limit will be replaced with a smaller message.
     * @param maxMessageSize
     *  max number of characters allowed in the JSON text
     */
    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    /**
     * Gets the limit on the message size
     * @return max number of characters allowed in the JSON text
     */
    public int getMaxMessageSize() {
        return this.maxMessageSize;
    }
}
