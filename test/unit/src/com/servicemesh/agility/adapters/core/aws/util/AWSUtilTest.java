/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import com.servicemesh.agility.api.Asset;
import com.servicemesh.agility.api.Link;

public class AWSUtilTest {
	
	@Test
	public void testConstructor() {
		assertTrue(new AWSUtil() instanceof AWSUtil);
	}
	
    @Test
    public void testIsValued() throws Exception
    {
        Object o = null;
        String s = null;
        StringBuilder sb = null;
        List<String> list = null;
        Collection<String> col = null;

        assertFalse(AWSUtil.isValued(o));
        assertFalse(AWSUtil.isValued(s));
        assertFalse(AWSUtil.isValued(sb));
        assertFalse(AWSUtil.isValued(list));
        assertFalse(AWSUtil.isValued(col));

        list = new ArrayList<String>();
        col = new TreeSet<String>();
        s = "";
        sb = new StringBuilder();

        assertFalse(AWSUtil.isValued(list));
        assertFalse(AWSUtil.isValued(col));
        assertFalse(AWSUtil.isValued(s));
        assertFalse(AWSUtil.isValued(sb));

        o = new Object();
        s = "string";
        sb = new StringBuilder("stringbuilder");
        list.add("some string");
        col.add("some string");

        assertTrue(AWSUtil.isValued(o));
        assertTrue(AWSUtil.isValued(s));
        assertTrue(AWSUtil.isValued(sb));
        assertTrue(AWSUtil.isValued(list));
        assertTrue(AWSUtil.isValued(col));
    }
    
    @Test
    public void testIsPositiveInt() {
    	assertTrue(AWSUtil.isPositive(1));
    	assertFalse(AWSUtil.isPositive(-1));
    }
    
    @Test
    public void testIsPositiveLong() {
    	long p = 1;
    	long n = -1;
    	assertTrue(AWSUtil.isPositive(p));
    	assertFalse(AWSUtil.isPositive(n));
    }
    
    @Test
    public void testLogObject() {
    	final TestAppender appender = new TestAppender();
        final Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
    	assertEquals("",AWSUtil.logObject("Test", logger));
    	assertEquals("",AWSUtil.logObject(null, null, null));
    	assertEquals("",AWSUtil.logObject("Test", null, null,true,2));
    	assertTrue(AWSUtil.logObject("Test", logger, Level.DEBUG).contains("String"));	
        assertTrue(AWSUtil.logObject("Test", logger, Level.INFO, true, 0).contains("String"));
        assertTrue(AWSUtil.logObject(new TestClass(), logger, Level.DEBUG, true, 1).contains("TestClass"));
        assertTrue(AWSUtil.logObject(new ExceptionClass(), logger, Level.DEBUG, true, 1).contains("ExceptionClass"));
        final List<LoggingEvent> log = appender.getLog();
        final LoggingEvent firstLogEntry = log.get(1);
        final LoggingEvent secondLogEntry = log.get(3);
        assertEquals(Level.DEBUG,firstLogEntry.getLevel());
        assertEquals(Level.INFO,secondLogEntry.getLevel());   
    }
    
    @Test
    public void testMaskPrivateKey() throws Exception
    {
        String completelyMasked = "*****";

        assertNull(AWSUtil.maskPrivateKey(null));
        assertNull(AWSUtil.maskPrivateKey(""));
        assertEquals(completelyMasked, AWSUtil.maskPrivateKey("a"));
        assertEquals(completelyMasked, AWSUtil.maskPrivateKey("ab"));
        assertEquals(completelyMasked, AWSUtil.maskPrivateKey("abc"));
        assertEquals(completelyMasked, AWSUtil.maskPrivateKey("abcd"));
        assertEquals(completelyMasked, AWSUtil.maskPrivateKey("abcde"));
        assertEquals("ab*****f", AWSUtil.maskPrivateKey("abcdef"));
        assertEquals("ab*****g", AWSUtil.maskPrivateKey("abcdefg"));
        assertEquals("ab*****h", AWSUtil.maskPrivateKey("abcdefgh"));
        assertEquals("ab*****i", AWSUtil.maskPrivateKey("abcdefghi"));
        assertEquals("ab*****j", AWSUtil.maskPrivateKey("abcdefghij"));
        assertEquals("abcd*****ghijk", AWSUtil.maskPrivateKey("abcdefghijk"));
        assertEquals("abcd*****hijkl", AWSUtil.maskPrivateKey("abcdefghijkl"));
        assertEquals("abcd*****ijklm", AWSUtil.maskPrivateKey("abcdefghijklm"));
        assertEquals("abcd*****jklmn", AWSUtil.maskPrivateKey("abcdefghijklmn"));
    }
    
    @Test
    public void testParseInt() {
    	final TestAppender appender = new TestAppender();
        final Logger logger = Logger.getRootLogger();
        logger.addAppender(appender);
        assertEquals(0, AWSUtil.parseInt(null));
        assertEquals(0, AWSUtil.parseInt(""));
        assertEquals(1, AWSUtil.parseInt("1"));
        AWSUtil.parseInt("Fail");
        final List<LoggingEvent> log = appender.getLog();
        for (LoggingEvent logEntry : log) {
        	String message = (String) logEntry.getMessage();
        	if (message.contains("locale=")) {
        		continue;
        	}
        	assertEquals(Level.ERROR,logEntry.getLevel());
    		assertTrue(message.contains("could not be converted"));
        }
		logger.removeAppender(appender);	
    }
    
    @Test
    public void testMakeLinkStub() {
    	assertNull(AWSUtil.makeLinkStub(null).getName());
    	Link out = AWSUtil.makeLinkStub("Test");
    	assertEquals("Test",out.getName());
    	assertEquals(0,out.getId());
    }
    
    @Test
    public void testMakeLinkStubWithID() {
    	Link out = AWSUtil.makeLinkStub(null, null);
    	assertNull(out.getName());
    	assertEquals(0,out.getId());
    	out = AWSUtil.makeLinkStub(1, "Test");
    	assertEquals(1,out.getId());
    	assertEquals("Test",out.getName());    	
    }
    
    @Test
    public void testGenerateId() {
    	assertTrue(AWSUtil.generateId(null).contains("sm-"));
    	assertTrue(AWSUtil.generateId("prefix").contains("prefix-"));
    }
    
    @Test
    public void testArrayToString() {
    	String[] array = new String[2];
    	array[0] = "a";
    	array[1] = "b";
    	assertEquals("a,b",AWSUtil.arrayToString(array));
    	assertEquals("",AWSUtil.arrayToString(null, null));
    	assertEquals("",AWSUtil.arrayToString(new String[0], null));
    	assertEquals("a,b",AWSUtil.arrayToString(array,null));
    }
    
	class TestAppender extends AppenderSkeleton {
	    private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();

	    @Override
	    public boolean requiresLayout() {
	        return false;
	    }

	    @Override
	    protected void append(final LoggingEvent loggingEvent) {
	        log.add(loggingEvent);
	    }

	    @Override
	    public void close() {
	    }

	    public List<LoggingEvent> getLog() {
	        return new ArrayList<LoggingEvent>(log);
	    }
	}
	
	class TestClass {
		public Asset getAsset() {
			return new Asset();
		}
		public String getPrivateKey() {
			return "testing";
		}
	}
	
	class ExceptionClass {
		public void isFail() throws Exception {
			throw new Exception("test ex");
		}
	}
	
}
