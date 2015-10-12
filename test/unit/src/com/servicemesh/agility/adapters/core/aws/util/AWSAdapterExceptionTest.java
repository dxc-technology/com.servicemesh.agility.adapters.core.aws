/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AWSAdapterExceptionTest {

	@Test
	public void testAWSAdapterException() {
		AWSAdapterException ex1 = new AWSAdapterException("message only");
		assertEquals("message only", ex1.getMessage());
		AWSAdapterException ex2 = new AWSAdapterException(new Throwable("throwable"));
		assertTrue(ex2.getMessage().contains("throwable"));
		AWSAdapterException ex3 = new AWSAdapterException("message",new Throwable("throwable"));
		assertTrue(ex3.getMessage().contains("message"));
	}
}
