/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class KeyValuePairTest {

	@Test
	public void testConstructor() {
		try {
			new KeyValuePair(null,"val");
			Assert.fail("shouldn't get here");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
		
		try {
			new KeyValuePair("","val");
			Assert.fail("shouldn't get here");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		}
		
		KeyValuePair kvp = new KeyValuePair("testkey","testval");
		String toS = kvp.toString();
		assertTrue(toS.contains("testkey"));
		assertTrue(toS.contains("testval"));
	}
	
	@Test
	public void testGetSet() {
		KeyValuePair kvp = new KeyValuePair();
		assertTrue(kvp.asMessage().contains("empty"));
		kvp.setKey("");
		assertTrue(kvp.asMessage().contains("empty"));
		kvp.setKey("testkey");
		kvp.setValue("value1");
		assertEquals("testkey",kvp.getKey());
		assertEquals("value1",kvp.getValue());
		String msg = kvp.asMessage();
		assertTrue(msg.contains("testkey"));
		assertTrue(msg.contains("value1"));
	}
}
