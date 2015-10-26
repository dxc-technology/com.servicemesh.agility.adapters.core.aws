/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.action;

import static org.mockito.Mockito.mock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.TestHelpers;
import com.servicemesh.core.async.CompletablePromise;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.async.PromiseFactory;
import com.servicemesh.core.messaging.Request;
import com.servicemesh.core.messaging.Response;
import com.servicemesh.core.messaging.Status;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.HttpStatus;
import com.servicemesh.io.http.HttpVersion;
import com.servicemesh.io.http.impl.DefaultHttpResponse;

public class TestStatusPoller
{
    @Before
    public void before()
    {
        TestHelpers.initLogger(Level.DEBUG);
    }

    private class PollerResponse extends Response
    {
        private static final long serialVersionUID = 20150715;
        public PollerResponse() {}
    }

    private static CompletablePromise<PollerResponse> _responsePromise = null;
    private static CompletablePromise<IHttpResponse> _runPromise = null;
    private static boolean _runAbort = false;

    private class Poller extends StatusPoller<PollerResponse>
    {
        private static final long serialVersionUID = 20150715;
        private Logger _logger = Logger.getLogger(Poller.class);
        private Object _decoded = null;
        private String _status;
        private boolean _failedState;
        private boolean _updateResponseAbort = false;

        public Poller(Request request, long interval, long retries, String
                      desiredState, AWSConnection conn, boolean retryOn404)
        {
            super(request, _responsePromise, interval, retries, desiredState,
                  conn, retryOn404);
        }

        @Override
        public Promise<IHttpResponse> run()
        {
            if (_runAbort)
                throw new RuntimeException("runAbort");
            return _runPromise;
        }

        @Override
        public Object decode(IHttpResponse response)
        {
            return _decoded;
        }

        public void setDecoded(Object decoded)
        {
            _decoded = decoded;
        }

        @Override
        public String getStatus(IHttpResponse response)
        {
            return _status;
        }

        public void setStatus(String status)
        {
            _status = status;
        }

        @Override
        public PollerResponse getResponseObject()
        {
            return new PollerResponse();
        }

        @Override
        public PollerResponse updateResponseObject(Request request, PollerResponse response, Object cloudObject)
        {
            if (_updateResponseAbort)
                throw new RuntimeException("updateResponseObjectAbort");
            return new PollerResponse();
        }

        public void setUpdateResponseAbort(boolean flag)
        {
            _updateResponseAbort = flag;
        }

        @Override
        public Logger getLogger()
        {
            return _logger;
        }

        @Override
        public boolean isFailedState(String statusValue)
        {
            return _failedState;
        }

        public void setFailedState(boolean failedState)
        {
            _failedState = failedState;
        }
    }

    @Test
    public void testPoller() throws Exception
    {
        //------------------------------------------------------------------
        // Construction scenarios
        //------------------------------------------------------------------

        Request request = null;
        long interval = -10;
        long retries = -10;
        String desiredState = null;
        AWSConnection mockConn = null;
        boolean retryOn404 = false;

        doConstruct("No responsePromise", request, null, interval,
                    retries, desiredState, mockConn, retryOn404, true);

        doConstruct("No request", request, interval, retries, desiredState,
                    mockConn, retryOn404, true);

        request = new Request();
        doConstruct("Bad interval", request, interval, retries, desiredState,
                    mockConn, retryOn404, true);

        interval = 1;
        doConstruct("Bad retries", request, interval, retries, desiredState,
                    mockConn, retryOn404, true);

        retries = 2;
        doConstruct("No connection", request, interval, retries, desiredState,
                    mockConn, retryOn404, true);

        mockConn = mock(AWSConnection.class);
        doConstruct("No desiredState", request, interval, retries,
                    desiredState, mockConn, retryOn404, true);

        desiredState = "OK";
        Poller poller = doConstruct("Good", request, interval, retries,
                                    desiredState, mockConn, retryOn404, false);

        //------------------------------------------------------------------
        // Fire scenarios
        //------------------------------------------------------------------

        poller.setDecoded(new String("hello"));
        poller.getLogger().setLevel(Level.TRACE);
        Status status = Status.FAILURE;
        doFire("No RunPromise", poller, false, status);

        retries = 1;
        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good2", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        poller.getLogger().setLevel(Level.INFO);
        status = null;
        doFire("RunPromise not complete", poller, false, status);

        status = Status.FAILURE;
        doFire("Retries exceeded", poller, false, status);

        retries = 10;
        retryOn404 = true;
        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good3", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        status = null;
        doFire("Pre-exc1", poller, false, status);
        DefaultHttpResponse response = new DefaultHttpResponse();
        response.setStatus(new HttpStatus(HttpVersion.HTTP_1_1, 404, ""));
        _runPromise.complete(response);
        _runAbort = true;
        status = Status.FAILURE;
        doFire("Polling status exception 1", poller, false, status);
        _runAbort = false;

        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good4", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        response = new DefaultHttpResponse();
        _runPromise.complete(response);
        doFire("No HttpResponse status", poller, false, status);

        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good5", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        response = new DefaultHttpResponse();
        response.setStatus(new HttpStatus(HttpVersion.HTTP_1_1, 200, ""));
        _runPromise.complete(response);
        poller.setStatus(desiredState + "-mismatch");
        status = null;
        doFire("200 response => no desired state", poller, false, status);
        poller.setFailedState(true);
        status = Status.FAILURE;
        doFire("200 response => failed state", poller, false, status);

        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good6", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        _runPromise.complete(response);
        poller.setStatus(desiredState);
        poller.setUpdateResponseAbort(true);
        doFire("200 response => updateResponse error ", poller, false, status);

        _runPromise = PromiseFactory.create();
        poller = doConstruct("Good7", request, interval, retries, desiredState,
                             mockConn, retryOn404, false);
        _runPromise.complete(response);
        poller.setStatus(desiredState);
        status = Status.COMPLETE;
        doFire("200 response => success ", poller, false, status);
    }

    private Poller doConstruct(String scenario, Request request,
                               long interval, long retries, String desiredState,
                               AWSConnection conn, boolean retryOn404,
                               boolean throwing)
    {
        _responsePromise = PromiseFactory.create();
        return doConstruct(scenario, request, _responsePromise, interval,
                           retries, desiredState, conn, retryOn404, throwing);
    }

    private Poller doConstruct(String scenario, Request request,
                               CompletablePromise<PollerResponse> respPromise,
                               long interval, long retries, String desiredState,
                               AWSConnection conn, boolean retryOn404,
                               boolean throwing)
    {
        _responsePromise = respPromise;
        Poller poller = null;
        try {
            poller = new Poller(request, interval, retries, desiredState, conn,
                                retryOn404);
            if (throwing)
                Assert.fail("Expected constructor failure: " + scenario);
        }
        catch (Exception e) {
            System.out.println("doConstruct " + scenario + ": " + e);
            if (! throwing)
                Assert.fail("Unexpected constructor exception: " + e);
        }
        return poller;
    }

    private long doFire(String scenario, Poller poller,
                        boolean throwing, Status status) throws Exception
    {
        long schedTime = 1, actualTime = 1, timerInterval = 0;
        try {
            timerInterval = poller.timerFire(schedTime, actualTime);

            if (throwing)
                Assert.fail("Expected timerFire failure: " + scenario);

            if (status != null) {
                Assert.assertFalse(_responsePromise.isFailed());
                Assert.assertTrue(_responsePromise.isCompleted());
                try {
                    PollerResponse pr = _responsePromise.get();
                    Assert.assertEquals(status, pr.getStatus());
                    System.out.println("doFire " + scenario + ": " +
                                       pr.getMessage());
                }
                catch (Throwable t) {}
            }
            else {
                Assert.assertFalse(_responsePromise.isFailed());
                Assert.assertFalse(_responsePromise.isCompleted());
            }
        }
        catch (Exception e) {
            System.out.println("doFire " + scenario + ": " + e);
            if (! throwing)
                Assert.fail("Unexpected timerFire exception: " + e);
        }
        return timerInterval;
    }
}
