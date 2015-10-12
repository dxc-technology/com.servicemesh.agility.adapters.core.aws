/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.servicemesh.agility.adapters.core.aws.util.AWSAdapterException;
import com.servicemesh.agility.adapters.core.aws.impl.AWSConnectionImpl;

import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.Cloud;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Link;
import com.servicemesh.agility.api.Property;
import com.servicemesh.agility.api.ServiceProvider;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpClientFactory;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.HttpStatus;
import com.servicemesh.io.http.HttpVersion;
import com.servicemesh.io.http.IHttpClient;
import com.servicemesh.io.http.IHttpClientConfig;
import com.servicemesh.io.http.IHttpClientConfigBuilder;
import com.servicemesh.io.http.IHttpHeader;
import com.servicemesh.io.http.IHttpRequest;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.http.impl.DefaultHttpRequest;
import com.servicemesh.io.http.impl.DefaultHttpResponse;
import com.servicemesh.io.proxy.Host;
import com.servicemesh.io.proxy.Proxy;
import com.servicemesh.io.proxy.ProxyType;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.*"})
@PrepareForTest({HttpClientFactory.class})
public class TestAWSConnection
{
    @Before
    public void before()
    {
        TestHelpers.setLogLevel(AWSConnectionImpl.class.getName(), Level.WARN);
    }

    @After
    public void after()
    {
        TestHelpers.setLogLevel(AWSConnectionImpl.class.getName(), Level.TRACE);
    }

    @Test
    public void testCredentials() throws Exception
    {
        AWSCredentialFactory cf = AWSCredentialFactory.getInstance();
        Assert.assertNotNull(cf);

        //---------------------------------------------------------------------
        // getCredentials(Cloud cloud)
        //---------------------------------------------------------------------

        Cloud cloud = new Cloud();
        cloud.setId(123);
        Credential cred = cf.getCredentials(cloud);
        Assert.assertNull(cred);

        Credential cloudCred = new Credential();
        cloud.setCloudCredentials(cloudCred);
        cred = cf.getCredentials(cloud);
        Assert.assertNull(cred);

        cloudCred.setPublicKey("foo");
        cred = cf.getCredentials(cloud);
        Assert.assertNull(cred);

        cloudCred.setPrivateKey("bar");
        cred = cf.getCredentials(cloud);
        Assert.assertSame(cloudCred, cred);

        //---------------------------------------------------------------------
        // getCredentials(ServiceProvider provider, List<Cloud> clouds)
        //---------------------------------------------------------------------

        ServiceProvider provider = new ServiceProvider();
        List<Cloud> clouds = null;
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        Link provCloud = new Link();
        provCloud.setId(cloud.getId());
        provider.setCloud(provCloud);
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        clouds = new ArrayList<Cloud>();
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        Credential cred2 = new Credential();
        Cloud cloud2 = new Cloud();
        cloud2.setId(456);
        cloud2.setCloudCredentials(cred2);

        clouds.add(cloud2);
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        clouds.add(cloud);
        cred = cf.getCredentials(provider, clouds);
        Assert.assertSame(cloudCred, cred);

        provCloud.setId(cloud2.getId());
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        cred2.setPrivateKey("blankPrivate");
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNull(cred);

        cred2.setPublicKey("blankPublic");
        cred = cf.getCredentials(provider, clouds);
        Assert.assertSame(cred2, cred);

        AssetProperty propAccessKey = new AssetProperty();
        propAccessKey.setName(AWSConfig.AWS_ACCESS_KEY);
        propAccessKey.setStringValue("propAccessKeyValue");
        AssetProperty propSecretKey = new AssetProperty();
        propSecretKey.setName(AWSConfig.AWS_SECRET_KEY);
        propSecretKey.setStringValue("propSecretKeyValue");
        provider.getProperties().add(propAccessKey);
        provider.getProperties().add(propSecretKey);
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNotNull(cred);
        Assert.assertEquals(propAccessKey.getStringValue(), cred.getPublicKey());
        Assert.assertEquals(propSecretKey.getStringValue(), cred.getPrivateKey());

        Credential provCred = new Credential();
        provider.setCredentials(provCred);
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNotNull(cred);
        Assert.assertEquals(propAccessKey.getStringValue(), cred.getPublicKey());

        provCred.setPublicKey("provCredPublic");
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNotNull(cred);
        Assert.assertEquals(propAccessKey.getStringValue(), cred.getPublicKey());

        provCred.setPrivateKey("provCredPrivate");
        cred = cf.getCredentials(provider, clouds);
        Assert.assertNotNull(cred);
        Assert.assertSame(provCred, cred);
    }

    @Test
    public void testConnImpl() throws Exception
    {
        List<Property> settings = null;
        Credential cred = null;
        Proxy proxy = null;
        AWSEndpoint ep = null;

        AWSConnection conn = construct("Null credential", settings, cred,
                                       proxy, ep, false);

        cred = new Credential();
        conn = construct("Cred with no keys", settings, cred, proxy, ep, false);

        cred.setPublicKey("foo");
        conn = construct("Cred with no private", settings, cred, proxy, ep, false);

        cred.setPrivateKey("bar");
        conn = construct("No endpoint", settings, cred, proxy, ep, false);

        ep = mock(AWSEndpoint.class);
        conn = construct("Endpoint with no version", settings, cred, proxy, ep, false);

        PowerMockito.mockStatic(HttpClientFactory.class);
        HttpClientFactory mockFactory = mock(HttpClientFactory.class);
        when(HttpClientFactory.getInstance()).thenReturn(mockFactory);
        IHttpClientConfigBuilder mockCB = mock(IHttpClientConfigBuilder.class);
        when(mockFactory.getConfigBuilder()).thenReturn(mockCB);
        IHttpClient mockClient = mock(IHttpClient.class);
        when(mockFactory.getClient(any(IHttpClientConfig.class)))
             .thenReturn(mockClient);
        IHttpHeader mockHeader = mock(IHttpHeader.class);
        when(mockFactory.createHeader(anyString(), anyString()))
             .thenReturn(mockHeader);

        when(ep.getAddress()).thenReturn("address");
        when(ep.getVersion()).thenReturn("2015-07-13");
        conn = construct("Good conn no proxy", settings, cred, proxy, ep, true);

        Host host = new Host("bar.com", 153);
        proxy = new Proxy("foo.com", 4223, ProxyType.HTTP_PROXY, host);
        conn = construct("Good conn with proxy", settings, cred, proxy, ep, true);

        doExecute(conn, ep, mockClient);
        doMethods(conn, ep);
    }

    private AWSConnection construct(String scenario, List<Property> settings,
                                    Credential cred, Proxy proxy,
                                    AWSEndpoint endpoint, boolean succeed)
        throws Exception
    {
        AWSConnection conn = null;
        try {
            conn = AWSConnectionFactory.getInstance()
                .getConnection(settings, cred, proxy, endpoint);
            if (succeed)
                Assert.assertNotNull(scenario, conn);
            else
                Assert.fail("Expected exception: " + scenario);
        }
        catch (AWSAdapterException aee) {
            if (succeed)
                Assert.fail("Unexpected exception: " + scenario);
        }
        return conn;
    }

    private void doExecute(AWSConnection conn, AWSEndpoint mockEndpoint,
                           IHttpClient mockClient)
        throws Exception
    {
        DefaultHttpResponse response = new DefaultHttpResponse();
        response.setStatus(new HttpStatus(HttpVersion.HTTP_1_1, 200, ""));
        Promise<IHttpResponse> promise = Promise.pure((IHttpResponse)response);
        when(mockClient.promise(any(IHttpRequest.class))).thenReturn(promise);

        QueryParams params = new QueryParams();
        when(mockEndpoint.getRegionName()).thenReturn("us-east-1");
        when(mockEndpoint.getServiceName()).thenReturn("s3");
        Promise<IHttpResponse> execPromise =
            conn.execute(HttpMethod.GET, params, IHttpResponse.class);
        Assert.assertNotNull(execPromise);
        Assert.assertTrue(! execPromise.isFailed());

        execPromise = conn.execute(HttpMethod.GET, null, IHttpResponse.class);
        Assert.assertNotNull(execPromise);
        Assert.assertTrue(execPromise.isFailed());
    }

    private void doMethods(AWSConnection conn, AWSEndpoint ep) throws Exception
    {
        IHttpRequest request = new DefaultHttpRequest(HttpMethod.GET);
        Whitebox.invokeMethod(conn, "addHeader", request, "foo", null);
        List<IHttpHeader> headers = request.getHeaders();
        Assert.assertTrue(headers.isEmpty());
        Whitebox.invokeMethod(conn, "addHeader", request, "foo", "bar");
        headers = request.getHeaders();
        Assert.assertFalse(headers.isEmpty());

        String resourceString = null;
        QueryParams params = null;
        URI uri = Whitebox.invokeMethod(conn, "getURI", resourceString, params);
        Assert.assertEquals(ep.getAddress(), uri.toString());

        resourceString = "";
        uri = Whitebox.invokeMethod(conn, "getURI", resourceString, params);
        Assert.assertEquals(ep.getAddress(), uri.toString());

        resourceString = "foo";
        uri = Whitebox.invokeMethod(conn, "getURI", resourceString, params);
        Assert.assertEquals(ep.getAddress() + "/foo", uri.toString());

        Map<String, String> cqpHeaders = new HashMap<String, String>();
        params = new QueryParams();
        String requestURI = "";
        String content = null;
        cqpHeaders.put("nonAmzHdr", "nonAmzValue");
        Whitebox.invokeMethod(conn, "completeQueryParams", cqpHeaders, params,
                              HttpMethod.GET, requestURI, content);
        String qpStr = params.toString();
        Assert.assertTrue(! qpStr.contains("nonAmz"));

        String hashStr = null;
        String hash = (String)Whitebox.invokeMethod(conn, "getHash", hashStr);
        Assert.assertTrue((hash != null) && (! hash.isEmpty()));

        byte[] hashData = null;
        try {
            Whitebox.invokeMethod(conn, "getHashFromBytes", hashData);
            Assert.fail("Expected exception for invalid getHashFromBytes data");
        }
        catch (AWSAdapterException aae) {
        }

        byte[] key = null;
        try {
            Whitebox.invokeMethod(conn, "getHmacSHA", hashStr, key);
            Assert.fail("Expected exception for invalid getHmacSHA args");
        }
        catch (AWSAdapterException aae) {
        }

        try {
            String dateStamp = null;
            Whitebox.invokeMethod(conn, "getSignatureKey", hashStr, dateStamp,
                                  ep.getRegionName(), ep.getServiceName());
            Assert.fail("Expected exception for invalid getSignatureKey args");
        }
        catch (AWSAdapterException aae) {
        }
    }
}
