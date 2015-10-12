/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class KeyValuesTest {
	
	@Test
	public void testBasicGetSet() {
		KeyValues<String> kv = new KeyValues<String>();
		kv.setKey("testkey");
		assertFalse(kv.fullyValued());
		String[] values = {"test1","test2"};
		kv.setValue(values);
		assertEquals("testkey",kv.getKey());
		List<String> out = kv.getValues();
		assertEquals("test1",out.get(0));
		assertEquals("test2",out.get(1));
		String msg = kv.asMessage();
		assertTrue(msg.contains("testkey"));
		assertTrue(msg.contains("test1"));
		assertTrue(msg.contains("test2"));
		String toS = kv.toString();
		assertTrue(toS.contains("key"));
		assertTrue(toS.contains("test1"));
		assertTrue(toS.contains("test2"));
		assertTrue(kv.fullyValued());
		assertTrue(kv.hasValue());
		kv.addValue("test3");
		assertTrue(kv.toString().contains("test3"));
	}
	
	@Test
	public void testNullKeysValues() {
		KeyValues<String> kv = new KeyValues<String>();
		assertFalse(kv.hasValue());
		assertFalse(kv.fullyValued());
		assertTrue(kv.asMessage().contains("empty"));
		kv.addValue(null);
		assertFalse(kv.hasValue());
		kv.setValue(new String[0]);
		assertFalse(kv.hasValue());
		assertTrue(kv.asMessage().contains("empty"));
		kv.setKey("");
		assertFalse(kv.fullyValued());
	}
	
	@Test
	public void testConstructors() {
		KeyValues<String> kv = new KeyValues<String>("testkey");
		assertEquals("testkey",kv.getKey());
		assertEquals(0,kv.getValues().size());
		
		try {
			new KeyValues<String>(null,"val");
			Assert.fail("shouldn't get here");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
		
		kv = new KeyValues<String>("testkey","value1");
		assertEquals("testkey",kv.getKey());
		assertEquals("value1",kv.getValues().get(0));
		
		try {
			new KeyValues<String>(null,new LinkedList<String>());
			Assert.fail("shouldn't get here");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
		
		LinkedList<String> vals = new LinkedList<String>();
		vals.add("value2");
		kv = new KeyValues<String>("testkey",vals);
		assertEquals("testkey",kv.getKey());
		assertEquals("value2",kv.getValues().get(0));
	}
}
