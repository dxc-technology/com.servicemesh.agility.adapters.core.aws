/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.apache.log4j.Level;

import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParam;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.proxy.Proxy;

import com.amazonaws.cloudfront.doc._2015_07_27.DefaultCacheBehavior;
import com.amazonaws.cloudfront.doc._2015_07_27.DistributionConfig;
import com.amazonaws.cloudfront.doc._2015_07_27.DistributionList;
import com.amazonaws.cloudfront.doc._2015_07_27.Origins;

/** AWS CloudFront integration tests */
public class TestCloudFrontIntegration
{
    private static final Logger logger = Logger.getLogger(TestCloudFrontIntegration.class);
    private Credential _cred;
    private List<Property> _settings = null;
    private Proxy _proxy = null;
    private Map<String, String> _headers = new HashMap<String, String>();

    private static final String CLOUDFRONT_SERVICE = "cloudfront";
    private static final String CLOUDFRONT_HOST =
        CLOUDFRONT_SERVICE + ".amazonaws.com";

    @Before
    public void before()
    {
        // Only run these tests if AWS keys have been provided
        _cred = TestHelpers.getAWSCredential();
        Assume.assumeTrue(_cred != null);
        TestHelpers.initLogger(Level.TRACE);
    }

    @Test
    public void testCloudFront() throws Exception
    {
        String region = "us-east-1";
        String version = "2015-07-27";

        _headers.put("host", CLOUDFRONT_HOST);

        AWSConnection conn = getConnection(region, version);
        queryCloudFront(conn);

        writeDistribution(conn);
    }

    private AWSConnection getConnection(String regionName, String version) throws Exception
    {
        AWSEndpoint endpoint = null;
        try {
            endpoint = AWSEndpointFactory.getInstance()
                .getEndpoint("https://" + CLOUDFRONT_HOST, regionName, version,
                             DistributionList.class);
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

    private void queryCloudFront(AWSConnection conn) throws Exception
    {
        QueryParams params = new QueryParams();
        params.setCaseSensitive(false);
        params.setMaintainOrder(false);
        params.add(new QueryParam("Version", conn.getEndpoint().getVersion()));

        String uri = "/" + conn.getEndpoint().getVersion() + "/distribution";

        Promise<DistributionList> promise =
            conn.execute(HttpMethod.GET, uri, _headers, params,
                         null, DistributionList.class);
        DistributionList distList =
            TestHelpers.completePromise(promise, conn.getEndpoint(), true);
        Assert.assertNotNull(distList);
    }

    private void writeDistribution(AWSConnection conn) throws Exception
    {
        QueryParams params = new QueryParams();
        params.setCaseSensitive(false);
        params.setMaintainOrder(false);
        params.add(new QueryParam("Version", conn.getEndpoint().getVersion()));

        String uri = "/" + conn.getEndpoint().getVersion() + "/distribution";

        // Not a valid payload - just testing encoding and hash calculation
        DistributionConfig distConfig = new DistributionConfig();
        distConfig.setEnabled("false");
        distConfig.setComment("TestCloudFrontIntegration");
        distConfig.setCallerReference("test");
        distConfig.setDefaultCacheBehavior(new DefaultCacheBehavior());
        distConfig.setOrigins(new Origins());

        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.POST, uri, _headers, params,
                         distConfig, IHttpResponse.class);

        IHttpResponse response = null;
        try {
            response = promise.get();
        }
        catch (Throwable t) {}
        logger.trace("StatusCode=" + response.getStatusCode());
        Assert.assertEquals(400, response.getStatusCode());
        String reason = response.getContent();
        if (reason != null) {
            logger.trace("Reason=" + reason);
        }
    }
}

