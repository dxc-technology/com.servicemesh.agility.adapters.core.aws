package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

public class ResourcesTest
{

    @Test
    public void testConstructor()
    {
        assertTrue(new Resources() instanceof Resources);
    }

    @Test
    public void testGetString()
    {
        String out = Resources.getString("missingKey");
        System.out.println(out);
        assertNotNull(out);
        assertFalse(out.equals("missingKey"));
    }

    @Test
    public void testGetStringWithParams()
    {
        String out = Resources.getString("parseIntError", "testing");
        System.out.println(out);
        assertNotNull(out);
        assertFalse(out.equals("parseIntError"));
        assertTrue(out.contains("testing"));
    }

    @Test
    public void testGetStringWithIntParam() throws Throwable
    {
        String out = Resources.getString("badStatus", 400);
        System.out.println(out);
        assertTrue(out.contains("400"));
        out = Resources.getString(new Locale("en", "US"), "badStatus", 401);
        System.out.println(out);
        assertTrue(out.contains("401"));
    }

    @Test
    public void testGetStringSpecificLocale()
    {
        String out = Resources.getString(new Locale("en", "US"), "missingKey");
        System.out.println(out);
        assertNotNull(out);
        assertFalse(out.equals("missingKey"));
    }

}
