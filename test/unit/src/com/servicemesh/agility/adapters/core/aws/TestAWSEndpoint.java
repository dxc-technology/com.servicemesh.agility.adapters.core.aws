/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;

import com.servicemesh.agility.adapters.core.aws.impl.AWSEndpointImpl;
import com.servicemesh.agility.adapters.core.aws.util.AWSAdapterException;
import com.servicemesh.agility.adapters.core.aws.util.AWSError;
import com.servicemesh.agility.adapters.core.aws.util.AWSErrorException;

import com.servicemesh.io.http.HttpStatus;
import com.servicemesh.io.http.HttpVersion;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.impl.DefaultHttpResponse;

import com.amazonaws.rds.doc._2010_07_28.Parameter;
import com.amazonaws.rds.doc._2010_07_28.ResponseMetadata;
import com.amazonaws.s3.doc._2006_03_01.LocationConstraint;

public class TestAWSEndpoint
{
    @Before
    public void before()
    {
        TestHelpers.initLogger(Level.TRACE);
    }

    @Test
    public void testContext() throws Exception
    {
        AWSEndpointFactory epf = AWSEndpointFactory.getInstance();

        //---------------------------------------------------------------------
        // Context creation/lookup when multiple contexts in scope
        //---------------------------------------------------------------------

        String addressS3 = "https://s3.us-east-1.amazonaws.com";
        AWSEndpoint epS3 = null;
        try {
            epf.getEndpoint(addressS3, "2006-03-01",
                            AWSAdapterException.class);
            Assert.fail("Expected exception for endpoint with non-JAXB class");
        }
        catch (AWSAdapterException aae) {
        }
        Assert.assertNull(epS3);

        epS3 = epf.getEndpoint(addressS3, "2006-03-01",
                               LocationConstraint.class);
        Assert.assertNotNull(epS3);

        String addressRDS = "https://rds.us-west-1.amazonaws.com";
        AWSEndpoint epRDS = epf.getEndpoint(addressRDS, "2010-07-28",
                                            Parameter.class);
        Assert.assertNotNull(epRDS);

        JAXBContext contextEP = epRDS.getContext();
        Assert.assertNotNull(contextEP);

        JAXBContext contextS3 =
            epf.lookupContext(LocationConstraint.class.getPackage().getName());
        Assert.assertNotNull(contextS3);
        Assert.assertNotSame(contextEP, contextS3);

        JAXBContext contextRDS =
            epf.lookupContext(Parameter.class.getPackage().getName());
        Assert.assertNotNull(contextRDS);
        Assert.assertSame(contextEP, contextRDS);

        String rmPath = ResponseMetadata.class.getPackage().getName();
        contextEP = epRDS.getContext(rmPath);
        Assert.assertSame(contextEP, contextRDS);

        JAXBContext noContext = epf.lookupContext("foo.bar");
        Assert.assertNull(noContext);

        //---------------------------------------------------------------------
        // Encoding/decoding
        //---------------------------------------------------------------------

        ResponseMetadata rm = new ResponseMetadata();
        rm.setRequestId("my meta rid");

        String encode1 = epRDS.encode(rm);
        Assert.assertNotNull(encode1);

        TestHelpers.setLogLevel(AWSEndpointImpl.class.getName(), Level.INFO);
        String encode2 = epRDS.encode(rmPath, rm);
        Assert.assertEquals(encode1, encode2);
        TestHelpers.setLogLevel(AWSEndpointImpl.class.getName(), Level.TRACE);

        try {
            epRDS.encode("foo.bar", rm);
            Assert.fail("Expected exception for encode with bad path");
        }
        catch (AWSAdapterException aae) {
        }

        AWSAdapterException ex = new AWSAdapterException("hey");
        try {
            epRDS.encode(ex);
            Assert.fail("Expected exception for non-JAXB object");
        }
        catch (AWSAdapterException aae) {
        }

        DefaultHttpResponse response = new DefaultHttpResponse();
        response.setStatus(new HttpStatus(HttpVersion.HTTP_1_1, 200, ""));
        response.setContent(encode1.getBytes());
        IHttpResponse iResponse = epRDS.decode(response, IHttpResponse.class);
        Assert.assertSame(response, iResponse);

        TestHelpers.setLogLevel(AWSEndpointImpl.class.getName(), Level.INFO);
        ResponseMetadata rmdec = epRDS.decode(response, rmPath,
                                              ResponseMetadata.class);
        Assert.assertNotNull(rmdec);
        Assert.assertEquals(rm.getRequestId(), rmdec.getRequestId());
        TestHelpers.setLogLevel(AWSEndpointImpl.class.getName(), Level.TRACE);

        try {
            epRDS.decode(response, Parameter.class);
            Assert.fail("Expected exception for decode invalid class");
        }
        catch (AWSAdapterException aae) {
        }

        try {
            epRDS.decode(response, AWSAdapterException.class.getPackage().getName(),
                         AWSAdapterException.class);
            Assert.fail("Expected exception for decode invalid context");
        }
        catch (AWSAdapterException aae) {
        }

        try {
            epS3.decode(response, ResponseMetadata.class);
            Assert.fail("Expected exception for decode invalid class for context");
        }
        catch (AWSAdapterException aae) {
        }

        //---------------------------------------------------------------------
        // Context unregister
        //---------------------------------------------------------------------

        epf.unregisterContext("foo.bar");
        contextRDS = epf.lookupContext(Parameter.class.getPackage().getName());
        Assert.assertNotNull(contextRDS);
        epf.unregisterContext(Parameter.class.getPackage().getName());
        contextRDS = epf.lookupContext(Parameter.class.getPackage().getName());
        Assert.assertNull(contextRDS);
    }

    @Test
    public void testErrorResponses() throws Exception
    {
        AWSEndpointFactory epf = AWSEndpointFactory.getInstance();
        String address = "https://rds.us-west-1.amazonaws.com";
        AWSEndpoint ep = epf.getEndpoint(address, "2010-07-28", Parameter.class);
        Assert.assertNotNull(ep);
        HttpStatus status100 = new HttpStatus(HttpVersion.HTTP_1_1, 100, "");
        HttpStatus status400 = new HttpStatus(HttpVersion.HTTP_1_1, 400, "");

        List<AWSError> expected = null;
        getErrors("No status", ep, null, null, expected);
        getErrors("Null content", ep, status100, null, expected);
        getErrors("Empty content", ep, status400, "", expected);
        getErrors("No error info", ep, status400, "hello", expected);
        getErrors("No /error", ep, status400, "<Error>", expected);

        expected = new ArrayList<AWSError>();
        AWSError error = new AWSError();
        expected.add(error);
        getErrors("No data", ep, status400, toContent(error), expected);

        error.setRequestId("my requestId");
        getErrors("Data 1", ep, status400, toContent(error), expected);

        error.setResource("my resource");
        getErrors("Data 2", ep, status400, toContent(error), expected);

        error.setMessage("my message");
        getErrors("Data 3", ep, status400, toContent(error), expected);

        error.setCode("my code");
        getErrors("Data 4", ep, status400, toContent(error), expected);

        String garbledError = toContent(error).replace("</Code>", "<Code>")
            .replaceFirst("<Code>", " </Code>");
        error.setCode(null);
        getErrors("Garbled", ep, status400, garbledError, expected);
        error.setCode("my code");

        AWSError error2 = new AWSError();
        expected.clear();
        expected.add(error2);
        error2.setCode("my code 2");
        garbledError = toContent(error2).replace("</Code>", "</Error></Code>");
        error2.setCode(null);
        getErrors("Garbled2", ep, status400, garbledError, expected);

        error2.setCode("error code 2");
        expected.add(error);
        AWSError error3 = new AWSError();
        expected.add(error3);
        error3.setMessage("error message 3");

        StringBuilder multi = new StringBuilder("<Response>");
        multi.append("\n<Errors>").append(toContent(error2)).append("\n")
            .append(toContent(error)).append("\n").append(toContent(error3))
            .append("\n</Errors>\n</Response>");
        getErrors("Multiple errors", ep, status400, multi.toString(), expected);
    }

    private void getErrors(String scenario, AWSEndpoint ep, HttpStatus status,
                           String content, List<AWSError> expected)
    {
        DefaultHttpResponse response = new DefaultHttpResponse();
        response.setStatus(status);
        if (content != null)
            response.setContent(content.getBytes());

        try {
            ep.decode(response, Parameter.class);
            Assert.fail("Expected exception: " + scenario);
        }
        catch (AWSAdapterException aae) {
            Assert.assertTrue(scenario, expected == null);
        }
        catch (AWSErrorException aee) {
            Assert.assertFalse(scenario, expected == null);

            List<AWSError> actual = aee.getErrors();
            Assert.assertNotNull(scenario, actual);
            if (expected.size() != actual.size()) {
                Assert.fail(scenario + ", expSz=" + expected.size() +
                            ", actSz=" + actual.size());
            }
            for (int i = 0 ; i < expected.size() ; i++) {
                AWSError expErr = expected.get(i);
                AWSError actErr = actual.get(i);
                Assert.assertEquals(scenario, expErr.toString(), actErr.toString());
            }
        }
    }

    private String toContent(AWSError error)
    {
        StringBuilder sb = new StringBuilder("<Error>");
        if (error.getCode() != null) {
            sb.append("<Code>").append(error.getCode()).append("</Code>");
        }
        if (error.getMessage() != null) {
            sb.append("<Message>").append(error.getMessage()).append("</Message>");
        }
        if (error.getResource() != null) {
            sb.append("<Resource>").append(error.getResource()).append("</Resource>");
        }
        if (error.getRequestId() != null) {
            sb.append("<RequestId>").append(error.getRequestId()).append("</RequestId>");
        }
        sb.append("</Error>");
        return sb.toString();
    }

    @Test
    public void testAddress() throws Exception
    {
        String address = "https://rds.us-west-1.amazonaws.com";
        tryAddress(address, true);
        address = "rds.us-west-1.amazonaws.com";
        tryAddress(address, true);
        address = "https:/rds.us-west-1.amazonaws.com";
        tryAddress(address, false);
        address = "https://";
        tryAddress(address, false);
        address = "https://noservice.";
        tryAddress(address, false);
        address = "noservice.";
        tryAddress(address, false);
        address = "https://host.noregion";
        tryAddress(address, false);
        address = "host.noregion";
        tryAddress(address, false);
        address = "https/malformed.us-west-1.amazonaws.com";
        tryAddress(address, false);
        address = "";
        tryAddress(address, false);
        address = null;
        tryAddress(address, false);
        address = "://noUriScheme";
        tryAddress(address, false);
        address = "ht://noUriScheme?malformed/param";
        tryAddress(address, false);

        address = "rds.amazonaws.com";
        String regionName = "us-west-1";
        String version = "2008-10-01";
        tryAddressRegion(address, regionName, version, true);
        regionName = null;
        tryAddressRegion(address, regionName, version, true); // yields amazonaws
        regionName = "";
        tryAddressRegion(address, regionName, version, true);
        address = "rds.com";
        regionName = "us-west-1";
        tryAddressRegion(address, regionName, version, true);
        regionName = null;
        tryAddressRegion(address, regionName, version, false);
        regionName = "";
        tryAddressRegion(address, regionName, version, false);
        regionName = "us-west-1";
        version = null;
        tryAddressRegion(address, regionName, version, false);
        version = "";
        tryAddressRegion(address, regionName, version, false);
        version = "2008-10-01";
        tryAddressRegion(address, regionName, version, true);

        String uriScheme = "http";
        String hostName = "rds.amazonaws.com";
        String serviceName = "rds";
        regionName = "us-east-1";
        int urlExpireSecs = AWSEndpoint.DEFAULT_URL_EXPIRE_SECS + 50;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, true);

        uriScheme = null;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        uriScheme = "";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        uriScheme = "http";
        hostName = null;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        hostName = "";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        hostName = "rds.amazonws.com";
        serviceName = null;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        serviceName = "";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        serviceName = "rds";
        regionName = null;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        regionName = "";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        regionName = "us-west-1";
        version = null;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        version = "";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, false);
        version = "2008-10-01";
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, true);
        urlExpireSecs = -30;
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, true);
        urlExpireSecs = 0;
        TestHelpers.setLogLevel(AWSEndpointImpl.class.getName(), Level.INFO);
        tryAddressUri(uriScheme, hostName, serviceName, regionName, version,
                      urlExpireSecs, true);
    }

    private void tryAddress(String address, boolean isGood) throws Exception
    {
        try {
            AWSEndpoint ep = AWSEndpointFactory.getInstance().getEndpoint(address, "2010-07-28", Parameter.class);

            if (! isGood)
                Assert.fail("Failure expected: " + address);
            Assert.assertNotNull(ep);
        }
        catch (AWSAdapterException aae) {
            if (isGood)
                Assert.fail("Exception for " + address + ": " + aae);
        }
    }

    private void tryAddressRegion(String address, String regionName, String version, boolean isGood) throws Exception
    {
        try {
            AWSEndpoint ep = AWSEndpointFactory.getInstance().getEndpoint(address, regionName, version, Parameter.class);

            if (! isGood)
                Assert.fail("Failure expected: address=" + address +
                            ", region=" + regionName + ", version=" + version);
            Assert.assertNotNull(ep);
        }
        catch (AWSAdapterException aae) {
            if (isGood)
                Assert.fail("Exception for address=" + address + ", region=" +
                            regionName + ", version=" + version + ": " + aae);
        }
    }

    private void tryAddressUri(String uriScheme, String hostName,
                               String serviceName, String regionName,
                               String version, int urlExpireSecs,
                               boolean isGood) throws Exception
    {
        try {
            AWSEndpoint ep = AWSEndpointFactory.getInstance().getEndpoint(uriScheme, hostName, serviceName, regionName, version, urlExpireSecs, Parameter.class);

            if (! isGood)
                Assert.fail("Failure expected: uriScheme=" + uriScheme +
                            ", hostName=" + hostName + ", serviceName=" +
                            serviceName + ", regionName=" + regionName +
                            ", version=" + version);
            Assert.assertNotNull(ep);

            int expSecs = (urlExpireSecs > 0) ?
                urlExpireSecs : AWSEndpoint.DEFAULT_URL_EXPIRE_SECS;
            Assert.assertEquals(expSecs, ep.getUrlExpireSecs());
        }
        catch (AWSAdapterException aae) {
            if (isGood)
                Assert.fail("Exception for uriScheme=" + uriScheme +
                            ", hostName=" + hostName + ", serviceName=" +
                            serviceName + ", regionName=" + regionName +
                            ", version=" + version + ": " + aae);
        }
    }
}
