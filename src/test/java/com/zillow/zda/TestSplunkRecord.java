package com.zillow.zda;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.lf5.LogRecord;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by weil1 on 12/22/16.
 */
public class TestSplunkRecord {
    Logger logger = Logger.getLogger(TestSplunkRecord.class);

    @Test
    public void shouldCreateExpectedJsonText() {
        LoggingEvent event = new LoggingEvent(TestSplunkRecord.class.getTypeName(), logger, 38905, Priority.ERROR, "message", new RuntimeException("test"));
        SplunkLayout layout = new SplunkLayout();
        String expect = "{\"tsi\":\"1969-12-31T16:00:38.9PST\",\"msg\":\"message\",\"lvl\":\"ERROR\",\"cls\":\"sun.reflect.NativeMethodAccessorImpl\",\"method\":\"invoke0\",\"file\":\"NativeMethodAccessorImpl.java\",\"name\":\"com.zillow.zda.TestSplunkRecord\",\"exception\":\"java.lang.RuntimeException:";
        String actual = layout.format(event);
        Assert.assertTrue(actual, actual.contains(expect));
    }

    @Test
    public void shouldNotCrashWithNullableFields() {
        LoggingEvent event = new LoggingEvent(TestSplunkRecord.class.getTypeName(), logger, 38905, Priority.ERROR, "message", null);
        SplunkLayout layout = new SplunkLayout();
        String actual = layout.format(event);
        String expect = "{\"tsi\":\"1969-12-31T16:00:38.9PST\",\"msg\":\"message\",\"lvl\":\"ERROR\",\"cls\":\"sun.reflect.NativeMethodAccessorImpl\",\"method\":\"invoke0\",\"file\":\"NativeMethodAccessorImpl.java\"";
        Assert.assertTrue(actual, actual.contains(expect));
    }

    @Test
    public void shouldTruncateMessage() {
        String longMessage = new String(new char[655350]).replace('\0', 'm');
        LoggingEvent event = new LoggingEvent(TestSplunkRecord.class.getTypeName(), logger, 38905, Priority.ERROR, longMessage, null);
        SplunkLayout layout = new SplunkLayout();
        layout.setMaxMessageSize(6000);
        String actual = layout.format(event);
        String expect1 = "{\"tsi\":\"1969-12-31T16:00:38.9PST\",\"msg\":\"Log message size:";
        String expect2 = "exceeding limit: 6000\",\"lvl\":\"ERROR\",\"cls\":\"sun.reflect.NativeMethodAccessorImpl\",\"method\":\"invoke0\",\"file\":\"NativeMethodAccessorImpl.java\"";
        Assert.assertTrue(actual, actual.contains(expect1));
        Assert.assertTrue(actual, actual.contains(expect2));
    }

    @Test
    public void shouldContainClusterAndHost() {
        SplunkRecord.setClusterName("regression");
        LoggingEvent event = new LoggingEvent(TestSplunkRecord.class.getTypeName(), logger, 38905, Priority.ERROR, "message", null);
        SplunkLayout layout = new SplunkLayout();
        String actual = layout.format(event);

        // Should also contain host name
        Assert.assertTrue(actual, actual.contains("host"));
        Assert.assertTrue(actual, actual.contains("\"cluster\":\"regression\""));
        Assert.assertTrue(actual, actual.contains("\"name\":\"" + TestSplunkRecord.class.getTypeName() + "\""));
    }
}
