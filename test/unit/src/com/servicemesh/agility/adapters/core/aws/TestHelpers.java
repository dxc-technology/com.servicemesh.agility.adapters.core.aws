/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;

import com.servicemesh.agility.adapters.core.aws.util.AWSAdapterException;
import com.servicemesh.agility.adapters.core.aws.util.AWSErrorException;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.HttpStatus;
import com.servicemesh.io.http.IHttpHeader;
import com.servicemesh.io.http.IHttpResponse;

public class TestHelpers
{
    private static final Logger _logger = Logger.getLogger(TestHelpers.class);
    private static final String _accessKey = System.getProperty("aws_access_key");
    private static final String _secretKey = System.getProperty("aws_secret_key");

    public static void initLogger(Level level)
    {
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(level);
        console.activateOptions();

        Logger logger = Logger.getLogger("com.servicemesh.agility.adapters.core.aws");
        logger.removeAllAppenders();
        logger.setLevel(Level.TRACE);
        logger.addAppender(console);
    }

    public static void setLogLevel(String loggerName, Level level)
    {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
    }

    public static Credential getAWSCredential()
    {
        Credential cred = null;
        StringBuilder sb = new StringBuilder();
        if ((_accessKey == null) || _accessKey.equals("${aws_access_key}")) {
            sb.append(" aws_access_key is not defined\n");
        }
        if ((_secretKey == null) || _secretKey.equals("${aws_secret_key}")) {
            sb.append(" aws_secret_key is not defined\n");
        }
        if (sb.length() > 0) {
            System.out.println("No AWS Credentials:" + sb.toString());
        }
        if (sb.length() == 0) {
            cred = new Credential();
            cred.setPublicKey(_accessKey);
            cred.setPrivateKey(_secretKey);
        }
        return cred;
    }

    public static <T> T completePromise(Promise<T> promise, AWSEndpoint endpoint, boolean expectSuccess) throws Exception
    {
        T obj = null;
        StringBuilder err = new StringBuilder();
        try {
            // No Reactor so just wait for completion
            obj = promise.get();
            if (obj instanceof IHttpResponse) {
                IHttpResponse response = (IHttpResponse) obj;
                HttpStatus status = response.getStatus();
                String reason = null;
                if (status.getReason() != null) {
                    reason = ", reason=" + status.getReason();
                }
                int code = response.getStatusCode();
                if (_logger.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("completePromise: IHttpResponse code=").append(code).append(reason)
                            .append(headersToString(response.getHeaders()));
                    _logger.trace(sb.toString());
                }
                if (expectSuccess && (code >= 400))
                    err.append("IHttpResponse code=").append(code).append("\n").append(response.getContent());
            }
            else {
                System.out.println("completePromise: obj=:\n" + endpoint.encode(obj));
            }
            if ((!expectSuccess) && (promise.isCompleted())) {
                err.append("completePromise: Expected failure");
            }
        }
        catch (AWSErrorException aex) {
            if (expectSuccess) {
                err.append("completePromise: AWSErrorException=" + aex.toString());
            }
            else {
                System.out.println("completePromise: Failed as expected: " + aex.toString());
            }
        }
        catch (AWSAdapterException aax) {
            if (expectSuccess) {
                err.append("completePromise: AWSAdapterException=" + aax.toString());
            }
            else {
                System.out.println("completePromise: Failed as expected: " + aax.toString());
            }
        }
        catch (Exception ex) {
            err.append("completePromise: Exception=" + ex);
        }
        catch (Throwable t) {
            err.append("completePromise: Throwable=" + t);
        }
        if (err.length() > 0) {
            if (_logger.isTraceEnabled()) {
                _logger.trace("completePromise err=" + err.toString());
            }
            Assert.fail(err.toString());
        }
        return obj;
    }

    public static String headersToString(List<IHttpHeader> headers)
    {
        StringBuilder sb = new StringBuilder();
        if (headers != null) {
            for (IHttpHeader header : headers) {
                sb.append("\n").append(header.getName()).append(":");

                String value = header.getValue();
                if ((value != null) && (!value.isEmpty()))
                    sb.append(value);
                else {
                    List<String> values = header.getValues();
                    boolean isFirst = true;
                    if (values != null) {
                        for (String v : values) {
                            if (isFirst) {
                                isFirst = false;
                                sb.append(v);
                            }
                            else
                                sb.append(",").append(v);
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String generateTestName(String prefix)
    //throws Exception
    {
        String generatedName;
        try {
            Thread.sleep(10);
        }
        catch (Exception ex) {
            // Ignore
        }

        generatedName = prefix + System.nanoTime();

        return generatedName;
    }
}
