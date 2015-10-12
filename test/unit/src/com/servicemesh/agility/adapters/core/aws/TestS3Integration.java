/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import org.apache.log4j.Level;

import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpHeader;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.proxy.Proxy;

import com.amazonaws.s3.doc._2006_03_01.CreateBucketConfiguration;
import com.amazonaws.s3.doc._2006_03_01.LocationConstraint;

/** AWS Simple Storage Service integration tests */
public class TestS3Integration
{
    private static final String S3_BUCKET_NAME = "smfy-test-s3-integration";

    // S3 API requires empty rather than null content so that "UNSIGNED-PAYLOAD"
    // is used as the content hash in the Canonical String
    private static final String EMPTY_CONTENT = "";

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
    public void testS3() throws Exception
    {
        String serviceName = "s3";
        String regionName = "us-east-1";
        String hostName = S3_BUCKET_NAME + "." + serviceName + ".amazonaws.com";
        String uriScheme = "https";
        String version = "2006-03-01";
        int urlExpireSecs = 86400;

        AWSEndpoint createEP = AWSEndpointFactory.getInstance()
            .getEndpoint(uriScheme, hostName, serviceName, regionName, version,
                         urlExpireSecs, CreateBucketConfiguration.class);
        AWSConnection createConn = AWSConnectionFactory.getInstance()
            .getConnection(_settings, _cred, _proxy, createEP);

        regionName = "us-west-1";
        hostName = S3_BUCKET_NAME + "." + serviceName + "-" +
            regionName + ".amazonaws.com";
        AWSEndpoint targetEP = AWSEndpointFactory.getInstance()
            .getEndpoint(uriScheme, hostName, serviceName, regionName, version,
                         urlExpireSecs, CreateBucketConfiguration.class);
        AWSConnection targetConn = AWSConnectionFactory.getInstance()
            .getConnection(_settings, _cred, _proxy, targetEP);

        boolean cleanup = true;
        try {
            createBucket(createConn, targetConn);
            verifyBucketAccess(targetConn);

            // Define some metadata that includes special characters. See
            // http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("x-amz-meta-author", "Shakespeare + *Blokes?");
            String stringObjName = "hello-world.txt";
            putObject(targetConn, stringObjName, "Hello, World", headers);
            getObject(targetConn, stringObjName, "Hello, World", headers);

            String binaryObjName = "my-array";
            byte[] binary = new byte[] { (byte)0x01, (byte)0x09, (byte)0xae };
            putObject(targetConn, binaryObjName, binary, headers);
            getObject(targetConn, binaryObjName, binary, headers);

            deleteObject(targetConn, stringObjName);
            // This should fail - binary object not deleted yet
            deleteBucket(targetConn, false);
            deleteObject(targetConn, binaryObjName);
            deleteBucket(targetConn, true);
            cleanup = false;
        }
        finally {
            if (cleanup) {
                try {
                    cleanupBucket(targetConn);
                }
                catch (Exception e) {}
            }
        }
    }

    private void createBucket(AWSConnection conn, AWSConnection targetConn)
        throws Exception
    {
        LocationConstraint locConstraint = new LocationConstraint();
        locConstraint.setValue(targetConn.getEndpoint().getRegionName());
        CreateBucketConfiguration bucketCfg = new CreateBucketConfiguration();
        bucketCfg.setLocationConstraint(locConstraint);

        Map<String, String> headers = new HashMap<String, String>();
        // Not needed: headers.put("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
        QueryParams params = new QueryParams();
        String requestURI = null;
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.PUT, requestURI, headers, params,
                         bucketCfg, IHttpResponse.class);
        TestHelpers.completePromise(promise, conn.getEndpoint(), true);
    }

    private void verifyBucketAccess(AWSConnection conn) throws Exception
    {
        QueryParams params = new QueryParams();
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.HEAD, null, null, params, EMPTY_CONTENT,
                         IHttpResponse.class);
        TestHelpers.completePromise(promise, conn.getEndpoint(), true);
    }

    private void putObject(AWSConnection conn, String objectName,
                           Object objectValue, Map<String, String> headers)
        throws Exception
    {
        QueryParams params = new QueryParams();
        String requestURI = "/" + objectName;
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.PUT, requestURI, headers, params,
                         objectValue, IHttpResponse.class);
        TestHelpers.completePromise(promise, conn.getEndpoint(), true);
    }

    private void getObject(AWSConnection conn, String objectName,
                           Object objectValue, Map<String, String> headers)
        throws Exception
    {
        QueryParams params = new QueryParams();
        String requestURI = "/" + objectName;
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.GET, requestURI, null, params,
                         EMPTY_CONTENT, IHttpResponse.class);
        IHttpResponse response =
            TestHelpers.completePromise(promise, conn.getEndpoint(), true);

        boolean foundHeader = false;
        for (IHttpHeader header : response.getHeaders()) {
            String actualName = header.getName();
            Assert.assertNotNull(actualName);

            for (Map.Entry<String, String> entry: headers.entrySet()) {
                if (actualName.equals(entry.getKey())) {
                    foundHeader = true;
                    String expectedValue = entry.getValue();
                    Assert.assertNotNull(expectedValue);
                    String actualValue = header.getValue();
                    Assert.assertNotNull(actualValue);
                    Assert.assertEquals(expectedValue, actualValue);
                    break;
                }
            }
            if (foundHeader)
                break;
        }
        Assert.assertTrue(foundHeader);
        if (objectValue instanceof String) {
            String content = response.getContent();
            Assert.assertNotNull(content);
            Assert.assertEquals((String)objectValue, content);
        }
        else {
            byte[] content = response.getContentAsByteArray();
            Assert.assertNotNull(content);
            Assert.assertArrayEquals((byte[])objectValue, content);
        }
    }

    private void deleteObject(AWSConnection conn, String objectName)
        throws Exception
    {
        QueryParams params = new QueryParams();
        String requestURI = "/" + objectName;
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.DELETE, requestURI, null, params,
                         EMPTY_CONTENT, IHttpResponse.class);
        IHttpResponse response =
            TestHelpers.completePromise(promise, conn.getEndpoint(), true);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getStatusCode() < 300); // Likely 204
    }

    private void deleteBucket(AWSConnection conn, boolean succeed) throws Exception
    {
        IHttpResponse response = cleanupBucket(conn);
        Assert.assertNotNull(response);
        if (succeed)
            Assert.assertTrue(response.getStatusCode() < 300); // Likely 204
        else
            Assert.assertTrue(response.getStatusCode() >= 300);
    }

    private IHttpResponse cleanupBucket(AWSConnection conn) throws Exception
    {
        QueryParams params = new QueryParams();
        Promise<IHttpResponse> promise =
            conn.execute(HttpMethod.DELETE, null, null, params, EMPTY_CONTENT,
                         IHttpResponse.class);
        try {
            return promise.get();
        }
        catch (Throwable t) {
            return null;
        }
    }
}
