/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.impl;

import java.net.URI;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import com.servicemesh.agility.adapters.core.aws.AWSConfig;
import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.AWSEndpoint;
import com.servicemesh.agility.adapters.core.aws.util.AWSAdapterException;
import com.servicemesh.agility.adapters.core.aws.util.AWSUtil;
import com.servicemesh.agility.adapters.core.aws.util.Resources;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;
import com.servicemesh.core.async.Function;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpClientFactory;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpClient;
import com.servicemesh.io.http.IHttpClientConfigBuilder;
import com.servicemesh.io.http.IHttpHeader;
import com.servicemesh.io.http.IHttpRequest;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParam;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.proxy.Proxy;

/**
 * Implements CRUD operations for Amazon Web Services Query APIs.
 */
public class AWSConnectionImpl implements AWSConnection
{
    private static final Logger _logger = Logger.getLogger(AWSConnectionImpl.class);

    // Using the AWS Signature Version 4 signing process
    private static final SimpleDateFormat SIGNING_DATE_FMT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat AWS_DATE_FMT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    static {
        SIGNING_DATE_FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        AWS_DATE_FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    private static final String SIGNING_ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String MAC_ALGORITHM = "HmacSHA256";

    private IHttpClient _httpClient;
    private AWSEndpoint _endpoint;
    private Credential _cred;

    /**
     * Creates an AWS connection.
     *
     * @param settings
     *            The configuration settings for the connection. Optional, may be empty or null.
     * @param cred
     *            Must be a credential that contains a public and private key.
     * @param proxy
     *            The proxy to be utilized. Optional, may be null.
     * @param endpoint
     *            Provides data specific to an AWS service.
     */
    public AWSConnectionImpl(List<Property> settings, Credential cred, Proxy proxy, AWSEndpoint endpoint) throws Exception
    {
        init(cred, settings, proxy, endpoint);
    }

    private void init(Credential cred, List<Property> settings, Proxy proxy, AWSEndpoint endpoint) throws Exception
    {
        if ((cred == null) || (!AWSUtil.isValued(cred.getPublicKey())) || (!AWSUtil.isValued(cred.getPrivateKey()))) {
            throw new AWSAdapterException(Resources.getString("missingCredential"));
        }
        _cred = cred;

        if ((endpoint == null) || (!AWSUtil.isValued(endpoint.getVersion()))) {
            throw new AWSAdapterException(Resources.getString("missingEndpoint"));
        }
        _endpoint = endpoint;

        IHttpClientConfigBuilder cb = HttpClientFactory.getInstance().getConfigBuilder();
        cb.setConnectionTimeout(AWSConfig.getConnectionTimeout(settings));
        cb.setRetries(AWSConfig.getRequestRetries(settings));
        cb.setSocketTimeout(AWSConfig.getSocketTimeout(settings));
        if (proxy != null) {
            cb.setProxy(proxy);
        }
        _httpClient = HttpClientFactory.getInstance().getClient(cb.build());
    }

    @Override
    public AWSEndpoint getEndpoint()
    {
        return _endpoint;
    }

    //-------------------------------------------------------------------------
    // QueryParams
    //-------------------------------------------------------------------------

    @Override
    public QueryParams initQueryParams(String action)
    {
        // AWS signature version 4: query string values must be
        // URL-encoded (space=%20). The parameters must be sorted by name.
        QueryParams qp = new QueryParams();
        qp.setCaseSensitive(false);
        qp.setMaintainOrder(false);
        qp.add(new QueryParam("Action", action));
        qp.add(new QueryParam("Version", _endpoint.getVersion()));
        return qp;
    }

    // Implements the bulk of the AWS signature version 4 signing process.
    private void completeQueryParams(Map<String, String> headers, QueryParams params, HttpMethod method, String requestURI,
            Object content) throws Exception
    {
        // Completed AWS signature version 4 example from AWS doc:
        // GET https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08&
        // X-Amz-Algorithm=AWS4-HMAC-SHA256&
        // X-Amz-Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request&
        // X-Amz-Date=20110909T233600Z&
        // X-Amz-SignedHeaders=content-type;host&
        // X-Amz-Signature=525d1a96c69b5549dd78dbbec8efe26410...9fd2 HTTP/1.1
        // Content-type: application/json
        // host: iam.amazonaws.com

        // For signed headers, AWS doc states: "the HTTP host header is
        // required. Any x-amz-* headers that you plan to add to the request
        // are also required for signature calculation."
        SortedMap<String, String> signedHeadersMap = new TreeMap<String, String>();
        signedHeadersMap.put("host", _endpoint.getHostName());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (key.startsWith("x-amz"))
                    signedHeadersMap.put(entry.getKey(), entry.getValue());
            }
        }
        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeaders = new StringBuilder();

        for (Map.Entry<String, String> entry : signedHeadersMap.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            if (signedHeaders.length() > 0) {
                signedHeaders.append(";");
            }
            signedHeaders.append(entry.getKey());
        }

        // Task 1: Create a Canonical Request For Signature Version 4
        // http://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
        Date now = Calendar.getInstance().getTime();
        String awsDate = AWS_DATE_FMT.format(now);
        String signingDate = SIGNING_DATE_FMT.format(now);

        String credScope = signingDate + "/" + _endpoint.getRegionName() + "/" + _endpoint.getServiceName() + "/aws4_request";
        String awsCred = _cred.getPublicKey() + "/" + credScope;

        params.add(new QueryParam("X-Amz-Algorithm", SIGNING_ALGORITHM));
        params.add(new QueryParam("X-Amz-Credential", awsCred));
        params.add(new QueryParam("X-Amz-Date", awsDate));
        params.add(new QueryParam("X-Amz-Expires", Integer.toString(_endpoint.getUrlExpireSecs())));
        params.add(new QueryParam("X-Amz-SignedHeaders", signedHeaders.toString()));

        // Strip the leading '?' for the canonical query string. Also,
        // QueryParam uses the java.net.URLEncoder with a UTF-8 encoding. AWS
        // documentation defines the modification of URLEncoder output to get
        // to RFC 3986 compliance:
        //    Do not URL-encode any of the unreserved characters that RFC 3986
        //    defines: A-Z, a-z, 0-9, hyphen ( - ), underscore ( _ ), period
        //    ( . ), and tilde ( ~ ). URLEncoder uses + for space, and won’t
        //    encode asterisk ( * ) , and encodes tilda ( ~ ) when not
        //    necessary.
        String canonicalQueryString = params.asQueryString().substring(1);
        if (canonicalQueryString.contains("+"))
            canonicalQueryString = canonicalQueryString.replace("+", "%20");
        if (canonicalQueryString.contains("*"))
            canonicalQueryString = canonicalQueryString.replace("*", "%2A");
        if (canonicalQueryString.contains("%7E"))
            canonicalQueryString = canonicalQueryString.replace("%7E", "~");

        // Amazon doc: "You don't include a payload hash in the Canonical
        // Request, because when you create a presigned URL, you don't know
        // anything about the payload. Instead, you use a constant string
        // "UNSIGNED-PAYLOAD"."
        String contentHash = (content != null) ? "UNSIGNED-PAYLOAD" : getHash("");

        if ((requestURI == null || (requestURI.isEmpty())))
            requestURI = "/";

        String canonicalRequest =
                method.getName() + "\n" + requestURI + "\n" + canonicalQueryString + "\n" + canonicalHeaders.toString() + "\n"
                        + signedHeaders.toString() + "\n" + contentHash;

        if (_logger.isTraceEnabled())
            _logger.trace("Canonical String:\n'" + canonicalRequest + "'");

        // Task 2: Create a String to Sign for Signature Version 4
        // http://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
        String stringToSign = SIGNING_ALGORITHM + "\n" + awsDate + "\n" + credScope + "\n" + getHash(canonicalRequest);

        if (_logger.isTraceEnabled())
            _logger.trace("String-to-Sign:\n'" + stringToSign + "'");

        // Task 3: Calculate the AWS Signature Version 4
        // http://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html
        byte[] signingKey =
                getSignatureKey(_cred.getPrivateKey(), signingDate, _endpoint.getRegionName(), _endpoint.getServiceName());

        String signature = Hex.encodeHexString(getHmacSHA(stringToSign.toString(), signingKey));

        // Task 4: Add the Signing Information to the Request
        // http://docs.aws.amazon.com/general/latest/gr/sigv4-add-signature-to-request.html
        params.add(new QueryParam("X-Amz-Signature", signature));
    }

    private String getHash(String value) throws Exception
    {
        if (value == null)
            value = "";
        return getHashFromBytes(value.getBytes());
    }

    private String getHashFromBytes(byte[] data) throws Exception
    {
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            md.update(data);
            return Hex.encodeHexString(md.digest());
        }
        catch (Exception e) {
            throw new AWSAdapterException(Resources.getString("failedGetHashFromBytes", e));
        }
    }

    private byte[] getHmacSHA(String data, byte[] key) throws Exception
    {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
            return mac.doFinal(data.getBytes(AWSEndpoint.CHAR_SET));
        }
        catch (Exception e) {
            throw new AWSAdapterException(Resources.getString("failedGetHmacSHA", e));
        }
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception
    {
        try {
            byte[] kSecret = ("AWS4" + key).getBytes(AWSEndpoint.CHAR_SET);
            byte[] kDate = getHmacSHA(dateStamp, kSecret);
            byte[] kRegion = getHmacSHA(regionName, kDate);
            byte[] kService = getHmacSHA(serviceName, kRegion);
            byte[] kSigning = getHmacSHA("aws4_request", kService);
            return kSigning;
        }
        catch (Exception e) {
            throw new AWSAdapterException(Resources.getString("failedGetSignature", e));
        }
    }

    //-------------------------------------------------------------------------
    // HTTP methods
    //-------------------------------------------------------------------------

    @Override
    public <T> Promise<T> execute(QueryParams params, final Class<T> responseClass)
    {
        return doExecute(HttpMethod.GET, null, null, params, null, responseClass);
    }

    @Override
    public <T> Promise<T> execute(HttpMethod method, QueryParams params, final Class<T> responseClass)
    {
        return doExecute(method, null, null, params, null, responseClass);
    }

    @Override
    public Promise<IHttpResponse> execute(HttpMethod method, QueryParams params)
    {
        return doExecute(method, null, null, params, null, IHttpResponse.class);
    }

    @Override
    public <T> Promise<T> execute(HttpMethod method, String requestURI, Map<String, String> headers, QueryParams params,
            Object resource, final Class<T> responseClass)
    {
        return doExecute(method, requestURI, headers, params, resource, responseClass);
    }

    @SuppressWarnings("unchecked")
    private <T> Promise<T> doExecute(HttpMethod method, String requestURI, Map<String, String> headers, QueryParams params,
            Object resource, final Class<T> responseClass)
    {
        URI uri = null;
        try {
            if (_logger.isTraceEnabled()) {
                StringBuilder trc = new StringBuilder();
                trc.append(method.getName()).append(" ").append(_endpoint.getHostName()).append(" ");

                if (requestURI != null)
                    trc.append(requestURI).append(" ");

                trc.append(_endpoint.getVersion());
                _logger.trace(trc.toString());
            }
            completeQueryParams(headers, params, method, requestURI, resource);
            uri = getURI(requestURI, params);
            IHttpRequest request = HttpClientFactory.getInstance().createRequest(method, uri);

            if (resource != null) {
                if (resource instanceof java.lang.String) {
                    request.setContent((String) resource);
                }
                else if (resource instanceof byte[]) {
                    request.setContent((byte[]) resource);
                }
                else {
                    request.setContent(_endpoint.encode(resource));
                    addContentTypeHeader(request);
                }
            }

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    addHeader(request, entry.getKey(), entry.getValue());
                }
            }

            if (_logger.isDebugEnabled()) {
                _logger.debug(method.getName() + " " + uri);
            }
            Promise<IHttpResponse> promise = _httpClient.promise(request);

            if (responseClass.getCanonicalName().equals(IHttpResponse.class.getCanonicalName())) {
                return (Promise<T>) promise;
            }
            else {
                return promise.map(new Function<IHttpResponse, T>() {
                    @Override
                    public T invoke(IHttpResponse response)
                    {
                        return _endpoint.decode(response, responseClass);
                    }
                });
            }
        }
        catch (Exception e) {
            String err = Resources.getString("executeException", method.getName(), uri, e.toString());
            _logger.error(err, e);
            return Promise.pure(new Exception(err));
        }
    }

    //-------------------------------------------------------------------------
    // Utility methods
    //-------------------------------------------------------------------------

    private URI getURI(String resourceString, QueryParams params) throws Exception
    {
        StringBuilder sb = new StringBuilder(_endpoint.getAddress());

        if (resourceString != null && !resourceString.isEmpty()) {
            if (resourceString.charAt(0) != '/') {
                sb.append("/");
            }
            sb.append(resourceString);
        }
        if (params != null) {
            sb.append(params.asQueryString());
        }
        return new URI(sb.toString());
    }

    private void addContentTypeHeader(IHttpRequest request)
    {
        addHeader(request, "Content-Type", _endpoint.getContentType());
    }

    private void addHeader(IHttpRequest request, String name, String value)
    {
        if (value != null) {
            IHttpHeader header = HttpClientFactory.getInstance().createHeader(name, value);
            request.setHeader(header);
        }
    }
}
