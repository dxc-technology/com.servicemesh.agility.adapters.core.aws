/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.apache.log4j.Level;

import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.proxy.Proxy;
import com.amazonaws.elasticloadbalancing.doc._2012_06_01.DescribeLoadBalancerPolicyTypesResponse;
import com.amazonaws.elasticloadbalancing.doc._2012_06_01.DescribeLoadBalancerPolicyTypesResult;
import com.amazonaws.elasticloadbalancing.doc._2012_06_01.PolicyTypeDescription;
import com.amazonaws.elasticloadbalancing.doc._2012_06_01.PolicyTypeDescriptions;

/** AWS Elastic Load Balancing integration tests */
public class TestELBIntegration
{
    private Credential _cred;
    private List<Property> _settings = null;
    private Proxy _proxy = null;

    @Before
    public void before()
    {
        // Only run these tests if AWS keys have been provided
        _cred = TestHelpers.getAWSCredential();
        Assume.assumeTrue(_cred != null);
        TestHelpers.initLogger(Level.TRACE);
    }

    @Test
    public void testELB() throws Exception
    {
        String address = "https://elasticloadbalancing.us-east-1.amazonaws.com";
        String region = null;
        String version = "2012-06-01";

        AWSConnection conn = getConnection(address, region, version);
        queryELB(conn);

        address = "elasticloadbalancing.us-east-1.amazonaws.com";
        conn = getConnection(address, region, version);
        queryELB(conn);

        address = "https://elasticloadbalancing.amazonaws.com";
        region = "us-east-1";
        conn = getConnection(address, region, version);
        queryELB(conn);

        address = "elasticloadbalancing.amazonaws.com";
        region = "us-east-1";
        conn = getConnection(address, region, version);
        queryELB(conn);
    }

    private AWSConnection getConnection(String address, String regionName, String version) throws Exception
    {
        AWSEndpoint endpoint = null;
        try {
            if (regionName == null) {
                endpoint = AWSEndpointFactory.getInstance()
                    .getEndpoint(address, version,
                                 DescribeLoadBalancerPolicyTypesResponse.class);
            }
            else {
                endpoint = AWSEndpointFactory.getInstance()
                    .getEndpoint(address, regionName, version,
                                 DescribeLoadBalancerPolicyTypesResponse.class);
            }
        }
        catch (Exception e) {
            Assert.fail("getEndpoint failed: " + e);
        }
        AWSConnection connection = null;
        try {
            connection = AWSConnectionFactory.getInstance()
                .getConnection(_settings, _cred, _proxy, endpoint);
        }
        catch (Exception e) {
            Assert.fail("getConnection failed: " + e);
        }
        return connection;
    }

    private void queryELB(AWSConnection conn) throws Exception
    {
        QueryParams params = conn.initQueryParams("DescribeLoadBalancerPolicyTypes");

        Promise<DescribeLoadBalancerPolicyTypesResponse> promise =
            conn.execute(params, DescribeLoadBalancerPolicyTypesResponse.class);
        DescribeLoadBalancerPolicyTypesResponse dlbptResponse =
            TestHelpers.completePromise(promise, conn.getEndpoint(), true);
        DescribeLoadBalancerPolicyTypesResult dlbptResult =
            dlbptResponse.getDescribeLoadBalancerPolicyTypesResult();
        Assert.assertNotNull(dlbptResponse);
        PolicyTypeDescriptions ptds = dlbptResult.getPolicyTypeDescriptions();
        Assert.assertNotNull(ptds);
        List<PolicyTypeDescription> ptdList = ptds.getMember();
        Assert.assertFalse(ptdList.isEmpty());

        // Try an error case
        params = conn.initQueryParams("BadZoot");
        promise = conn.execute(params, DescribeLoadBalancerPolicyTypesResponse.class);
        TestHelpers.completePromise(promise, conn.getEndpoint(), false);
    }
}

