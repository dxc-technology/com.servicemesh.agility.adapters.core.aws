/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.impl;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import com.servicemesh.agility.adapters.core.aws.AWSEndpoint;
import com.servicemesh.agility.adapters.core.aws.util.AWSAdapterException;
import com.servicemesh.agility.adapters.core.aws.util.AWSError;
import com.servicemesh.agility.adapters.core.aws.util.AWSErrorException;
import com.servicemesh.agility.adapters.core.aws.util.AWSUtil;
import com.servicemesh.agility.adapters.core.aws.util.Resources;
import com.servicemesh.io.http.HttpClientException;
import com.servicemesh.io.http.HttpStatus;
import com.servicemesh.io.http.HttpUtil;
import com.servicemesh.io.http.IHttpResponse;

/**
 * Implements operations for AWS Endpoints.
 */
public class AWSEndpointImpl implements AWSEndpoint
{
    private static final Logger _logger = Logger.getLogger(AWSEndpointImpl.class);

    private String _address;
    private String _hostName;
    private String _serviceName;
    private String _regionName;
    private String _version;
    private int _urlExpireSecs;
    private ClassLoader _contextLoader;
    private JAXBContext _context;

    private static class Holder
    {
        private static HashMap<String, JAXBContext> contextMap = new HashMap<String, JAXBContext>();
        private static final Object lock = new Object();

        private static JAXBContext getContext(String contextPath, ClassLoader loader)
        {
            JAXBContext context;
            synchronized (Holder.lock) {
                context = Holder.contextMap.get(contextPath);
                if (context == null) {
                    context = Holder.createContext(contextPath, loader);
                    Holder.contextMap.put(contextPath, context);
                }
            }
            return context;
        }

        private static JAXBContext createContext(String contextPath, ClassLoader loader)
        {
            try {
                return JAXBContext.newInstance(contextPath, loader);
            }
            catch (Exception ex) {
                AWSEndpointImpl._logger.error("createContext: " + contextPath + ", exception=" + ex);
                return null;
            }
        }

        private static JAXBContext lookupContext(String contextPath)
        {
            JAXBContext context = null;
            synchronized (Holder.lock) {
                context = Holder.contextMap.get(contextPath);
            }
            return context;
        }

        private static void unregisterContext(String contextPath)
        {
            synchronized (Holder.lock) {
                Holder.contextMap.remove(contextPath);
            }
        }
    }

    /**
     * Looks up the context for the given context path.
     *
     * @param contextPath
     *            The context path of the JAXBContext to look up.
     * @return The JAXBContext for the contextPath.
     */
    public static JAXBContext lookupContext(String contextPath)
    {
        return Holder.lookupContext(contextPath);
    }

    /**
     * Unregisters the given context.
     *
     * @param contextPath
     *            The path of the context to be unregistered.
     */
    public static void unregisterContext(String contextPath)
    {
        Holder.unregisterContext(contextPath);
    }

    /**
     * Creates an AWS endpoint.
     *
     * @param address
     *            The address of the AWS access point. See AWSEndpointFactory
     *            getEndpoint() methods.
     * @param regionName
     *            The AWS region name, e.g. "us-east-1". May be null, in which case it must be part of the address.
     * @param version
     *            The API version.
     * @param contextClass
     *            One of the JAXB classes for the AWS API. Used to initialize the default context for the endpoint.
     */
    public <T> AWSEndpointImpl(String address, String regionName, String version, Class<T> contextClass)
    {
        // Address must contain host, service, and region names. Expecting form
        // like "https://<serviceName>.<regionName>.<rest>" where
        // "<serviceName>.<regionName>.<rest>" is the hostName.
        // See examples on http://docs.aws.amazon.com/general/latest/gr/rande.html
        if (address == null)
            throw new AWSAdapterException(Resources.getString("emptyAddress"));

        // Default to HTTPS if no URI scheme is specified
        String fullAddress = (address.indexOf("://") > 0) ? address : "https://" + address;
        String hostName = parseHostName(fullAddress);
        String serviceName = null;
        int i1 = hostName.indexOf('.');

        if (i1 > 0) {
            serviceName = hostName.substring(0, i1);
            i1++;
            if (! AWSUtil.isValued(regionName)) {
                int i2 = hostName.indexOf('.', i1);
                if (i2 > i1) {
                    regionName = hostName.substring(i1, i2);
                }
            }
        }
        if (! AWSUtil.isValued(serviceName)) {
            throw new AWSAdapterException(Resources.getString("missingService", address));
        }
        if (! AWSUtil.isValued(regionName)) {
            throw new AWSAdapterException(Resources.getString("missingRegion", address));
        }
        if (! AWSUtil.isValued(version)) {
            throw new AWSAdapterException(Resources.getString("emptyVersion"));
        }

        init(fullAddress, hostName, serviceName, regionName, version, DEFAULT_URL_EXPIRE_SECS, contextClass);
    }

    /**
     * Creates an AWS endpoint.
     *
     * @param uriScheme
     *            The URI scheme, e.g. "https"
     * @param hostName
     *            The AWS host name.
     * @param serviceName
     *            The AWS service name.
     * @param regionName
     *            The AWS region name.
     * @param version
     *            The API version.
     * @param urlExpireSecs
     *            The expiration time in seconds for a signed URL.
     * @param contextClass
     *            One of the JAXB classes for the AWS API. Used to initialize the default context for the endpoint.
     */
    public <T> AWSEndpointImpl(String uriScheme, String hostName, String serviceName, String regionName, String version,
            int urlExpireSecs, Class<T> contextClass)
    {
        if (! AWSUtil.isValued(uriScheme)) {
            throw new AWSAdapterException(Resources.getString("emptyUriScheme"));
        }
        if (! AWSUtil.isValued(hostName)) {
            throw new AWSAdapterException(Resources.getString("emptyHostname"));
        }
        String address = uriScheme + "://" + hostName;
        hostName = parseHostName(address);

        if (! AWSUtil.isValued(serviceName)) {
            throw new AWSAdapterException(Resources.getString("emptyService"));
        }
        if (! AWSUtil.isValued(regionName)) {
            throw new AWSAdapterException(Resources.getString("emptyRegion"));
        }
        if (! AWSUtil.isValued(version)) {
            throw new AWSAdapterException(Resources.getString("emptyVersion"));
        }

        if (urlExpireSecs <= 0)
            urlExpireSecs = DEFAULT_URL_EXPIRE_SECS;

        init(address, hostName, serviceName, regionName, version, urlExpireSecs, contextClass);
    }

    private String parseHostName(String address)
    {
        String hostName = null;
        try {
            URL url = new URL(address);
            hostName = url.getHost();
        }
        catch (Exception ex) {
            throw new AWSAdapterException(Resources.getString("invalidAddress", address));
        }
        if (! AWSUtil.isValued(hostName)) {
            throw new AWSAdapterException(Resources.getString("missingHostname", address));
        }
        return hostName;
    }

    private <T> void init(String address, String hostName, String serviceName, String regionName, String version,
            int urlExpireSecs, Class<T> contextClass)
    {
        _address = address;
        _hostName = hostName;
        _serviceName = serviceName;
        _regionName = regionName;
        _version = version;
        _urlExpireSecs = urlExpireSecs;
        try {
            _contextLoader = contextClass.getClassLoader();
            String contextPath = contextClass.getPackage().getName();
            _context = Holder.getContext(contextPath, _contextLoader);
            if (_context == null) {
                throw new AWSAdapterException(Resources.getString("missingContext", contextClass.getName()));
            }
            if (_logger.isTraceEnabled()) {
                _logger.trace("init: address=" + _address + ", hostName=" +
                              _hostName + ", serviceName=" + _serviceName +
                              ", regionName=" + _regionName + ", version=" +
                              _version + ", urlExpireSecs=" + _urlExpireSecs);
            }
        }
        catch (Exception e) {
            throw new AWSAdapterException(Resources.getString("contextException", e));
        }
    }

    @Override
    public String getAddress()
    {
        return _address;
    }

    @Override
    public String getHostName()
    {
        return _hostName;
    }

    @Override
    public String getServiceName()
    {
        return _serviceName;
    }

    @Override
    public String getRegionName()
    {
        return _regionName;
    }

    @Override
    public String getContentType()
    {
        return AWSEndpoint.DEFAULT_CONTENT_TYPE;
    }

    @Override
    public String getVersion()
    {
        return _version;
    }

    @Override
    public int getUrlExpireSecs()
    {
        return _urlExpireSecs;
    }

    @Override
    public JAXBContext getContext()
    {
        return _context;
    }

    @Override
    public JAXBContext getContext(String contextPath)
    {
        return Holder.getContext(contextPath, _contextLoader);
    }

    @Override
    public <T> T decode(IHttpResponse response, Class<T> responseClass)
    {
        return doDecode(response, responseClass, _context);
    }

    @Override
    public <T> T decode(IHttpResponse response, String responseClassPath, Class<T> responseClass)
    {
        JAXBContext responseContext = getContext(responseClassPath);
        if (responseContext == null) {
            throw new AWSAdapterException(Resources.getString("missingDecodeContext", responseClassPath));
        }
        return doDecode(response, responseClass, responseContext);
    }

    private <T> T doDecode(IHttpResponse response, Class<T> responseClass, JAXBContext responseContext)
    {
        if (responseClass.isInstance(response)) {
            // We already have the return object
            return responseClass.cast(response);
        }
        StringBuilder err = new StringBuilder();
        int statusCode = response.getStatusCode();
        if ((statusCode < 200) || (statusCode >= 300)) {
            err.append(Resources.getString("badStatus", statusCode));
            handleError(err.toString(), response);
        }
        Object object = null;
        try {
            object = HttpUtil.decodeObject(response.getContent(), null, responseContext);
            if (_logger.isTraceEnabled()) {
                _logger.trace("decoded: " + response.getContent());
            }
        }
        catch (HttpClientException ex) {
            err.append(Resources.getString("decodeException", ex.getMessage()));
            handleError(err.toString(), response);
        }

        T responseObject = null;
        if (responseClass.isInstance(object)) {
            responseObject = responseClass.cast(object);
        }
        else {
            handleError(Resources.getString("unexpectedResponse"), response);
        }
        return responseObject;
    }

    private void handleError(String context, IHttpResponse response)
    {
        StringBuilder err = new StringBuilder(context);
        HttpStatus status = response.getStatus();
        if (status != null)
            err.append(" ").append(status.toString());

        List<AWSError> errors = getAWSErrors(response.getContent());
        if (!errors.isEmpty())
            throw new AWSErrorException(err.toString(), errors);
        else
            throw new AWSAdapterException(err.toString());
    }

    // Creates AWSError objects using parameters that can occur across one or
    // more AWS responses
    private List<AWSError> getAWSErrors(String content)
    {
        List<AWSError> errors = new ArrayList<AWSError>();
        int e1 = 0, e2 = 0;
        boolean done = (content == null);

        while (!done) {
            done = true;
            e1 = content.indexOf("<Error>", e1);
            if (e1 >= 0) {
                e2 = content.indexOf("</Error>", e1 + "<Error>".length());
                if (e2 > e1) {
                    done = false;
                    errors.add(parseError(content, e1, e2));
                    e1 = e2;
                }
            }
        }
        return errors;
    }

    private AWSError parseError(String content, int e1, int e2)
    {
        AWSError error = new AWSError();

        String value = getValue(content, "<Code>", "</Code>", e1, e2);
        if (value != null) {
            error.setCode(value);
        }

        value = getValue(content, "<Message>", "</Message>", e1, e2);
        if (value != null) {
            error.setMessage(value);
        }

        value = getValue(content, "<Resource>", "</Resource>", e1, e2);
        if (value != null) {
            error.setResource(value);
        }

        value = getValue(content, "<RequestId>", "</RequestId>", e1, e2);
        if (value != null) {
            error.setRequestId(value);
        }
        return error;
    }

    private String getValue(String content, String startTag, String endTag, int minIdx, int maxIdx)
    {
        String value = null;
        int iStart = content.indexOf(startTag, minIdx);
        if ((iStart >= 0) && (iStart < maxIdx)) {
            int iContent = iStart + startTag.length();
            int iEnd = content.indexOf(endTag, iContent);
            if ((iEnd > 0) && (iEnd < maxIdx)) {
                value = content.substring(iContent, iEnd);
            }
        }
        return value;
    }

    @Override
    public String encode(Object obj)
    {
        return doEncode(obj, _context);
    }

    @Override
    public String encode(String objClassPath, Object obj)
    {
        JAXBContext objContext = getContext(objClassPath);
        if (objContext == null) {
            throw new AWSAdapterException(Resources.getString("missingEncodeContext", objClassPath));
        }
        return doEncode(obj, objContext);
    }

    private String doEncode(Object obj, JAXBContext objContext)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = objContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(obj, os);
            if (_logger.isTraceEnabled()) {
                _logger.trace("encoded: " + os.toString());
            }
            return os.toString();
        }
        catch (Exception ex) {
            AWSEndpointImpl._logger.error("encode Exception: " + ex);
            throw new AWSAdapterException(Resources.getString("encodeException", ex));
        }
    }
}
