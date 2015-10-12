/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;

import org.junit.Test;

public class AWSErrorExceptionTest {
	
	@Test
	public void testAWSErrorException() {
		AWSError e = new AWSError();
		AWSErrorException err = new AWSErrorException((String)null, e);
		assertEquals(e,err.getErrors().get(0));
		System.out.println(err.toString());
		
		LinkedList<AWSError> list = new LinkedList<AWSError>();
		AWSErrorException err2 = new AWSErrorException("msg",list);
		System.out.println(err2.toString());
	}
}
