/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AWSErrorTest {

	@Test
	public void testAWSError() {
		AWSError error = new AWSError();
		error.setCode("testCode");
		error.setMessage("testMessage");
		error.setRequestId("testId");
		error.setResource("testResource");
		assertEquals("testCode",error.getCode());
		assertEquals("testMessage",error.getMessage());
		assertEquals("testId",error.getRequestId());
		assertEquals("testResource",error.getResource());
		assertTrue(error.toString().contains("testCode"));
		assertTrue(error.toString().contains("testId"));
		assertTrue(error.toString().contains("testMessage"));
		assertTrue(error.toString().contains("testResource"));
		
		AWSError error2 = new AWSError();
		assertTrue(error2.toString().isEmpty());
	}
}
