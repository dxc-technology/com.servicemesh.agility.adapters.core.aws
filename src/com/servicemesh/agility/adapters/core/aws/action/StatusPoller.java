/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.action;

import java.io.Serializable;
import java.text.MessageFormat;

import org.apache.log4j.Logger;

import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.util.AWSUtil;
import com.servicemesh.core.async.CompletablePromise;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.messaging.Request;
import com.servicemesh.core.messaging.Response;
import com.servicemesh.core.messaging.Status;
import com.servicemesh.core.reactor.TimerHandler;
import com.servicemesh.io.http.IHttpResponse;

public abstract class StatusPoller<T extends Response> implements TimerHandler, Serializable
{
    private static final long serialVersionUID = 20150325;

    protected static final MessageFormat MISSING_PARAM_MSG = new MessageFormat("The {0} parameter is required but missing.");
    protected static final MessageFormat NEGATIVE_PARAM_MSG = new MessageFormat("The {0} parameter cannot be negative.");

    protected final Request request; // the request that initiated the event
    protected final long interval; // interval between polls
    protected long retries; // max number of times the timer is allowed to be called
    protected final AWSConnection conn; // Azure connection object
    private Promise<?> promise; // Promise object from async HTTP call - used to monitor call
    protected final String desiredState; // the desired state of the monitored object
    private Object jaxbObject; // the object converted from the XML response
    private final boolean retryOn404; // Flag that determines if the poller should retry or fail when the response is 404
    protected final CompletablePromise<T> responsePromise;// Promise used to pass response to client

    /**
     * Constructor for Status Poller All params are required
     *
     * @param request
     *            The request that initiated the event
     * @param responsePromise
     *            Handler - Promise used to pass response to client
     * @param interval
     *            Interval between polls in millisecs
     * @param retries
     *            Max number of times the timer is allowed to be called
     * @param desiredState
     *            The desired state of the monitored object
     * @param conn
     *            Azure connection object to be used for the polling API call
     * @param retryOn404
     *            Flag that determines if the poller should retry or fail when the response is 404.
     */
    public StatusPoller(Request request, CompletablePromise<T> responsePromise, long interval, long retries, String desiredState,
    		AWSConnection conn, boolean retryOn404)
    {
        if (responsePromise != null) {
            this.responsePromise = responsePromise;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.MISSING_PARAM_MSG.format(new Object[] { "handler" }));
            throw e;
        }

        if (request != null) {
            this.request = request;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.MISSING_PARAM_MSG.format(new Object[] { "request" }));
            responsePromise.failure(e);
            throw e;
        }

        if (interval > -1) {
            this.interval = interval;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.NEGATIVE_PARAM_MSG.format(new Object[] { "interval" }));
            responsePromise.failure(e);
            throw e;
        }

        if (retries > -1) {
            this.retries = retries;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.NEGATIVE_PARAM_MSG.format(new Object[] { "retries" }));
            responsePromise.failure(e);
            throw e;
        }

        if (conn != null) {
            this.conn = conn;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.MISSING_PARAM_MSG.format(new Object[] { "connection" }));
            responsePromise.failure(e);
            throw e;
        }

        if (AWSUtil.isValued(desiredState)) {
            this.desiredState = desiredState;
        }
        else {
            IllegalArgumentException e =
                    new IllegalArgumentException(StatusPoller.MISSING_PARAM_MSG.format(new Object[] { "desiredState" }));
            responsePromise.failure(e);
            throw e;
        }

        this.retryOn404 = retryOn404;

        this.promise = run();
    }

    /**
     * This method will execute the required call to get a fresh response.
     *
     * @return Promise<?> - future HTTP response object
     */
    abstract protected Promise<?> run();

    /**
     * This method will decode the XML content into a JAXB object.
     *
     * @param httpResponse
     *            The response from the HTTP call
     * @return Object - the decoded object; null if the parameter is null
     */
    abstract protected Object decode(IHttpResponse httpResponse);

    /**
     * This method will will return the status value from the JAXB object.
     *
     * @param httpResponse
     *            The response from the HTTP call
     * @return String - the status value; null if the parameter is null
     */
    abstract protected String getStatus(IHttpResponse httpResponse);

    /**
     * This method will return a new response object to match the poller.
     *
     * @return StorageResponse - a new response object
     */
    abstract protected T getResponseObject();

    /**
     * This method will update the response object with the proper data from the JAXB object.
     *
     * @param request
     *            The request object for the operation
     * @param response
     *            The response object to be updated
     * @param cloudObject
     *            The JAXB cloud object
     * @return StorageResponse - the updated response object
     */
    abstract protected T updateResponseObject(Request request, T response, Object cloudObject);

    /**
     * This method will return the logger for this class
     *
     * @return Logger - logger for this class; used to properly log messages from parent methods
     */
    abstract protected Logger getLogger();

    /**
     * This is the method to call when the timer expires. The timer will continue to reset until the verification context is
     * verified or the verification context expires. If the context is verified, the status will be COMPLETE. If the context
     * expires, the status will be FAILURE.
     *
     * @param scheduledTime
     *         Time in milliseconds when the timer should fire
     * @param actualTime
     *         Time in milliseconds
     * @return long - next scheduled time for firing; 0 implies the timer is complete
     */
    @Override
    public long timerFire(long scheduledTime, long actualTime)
    {
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("The timer has fired for request " + request.getReqId());
        }
        T response = getResponseObject();
        Status status = null;
        String msg = null;
        long timerInterval = 0; // initialize to assume completion
        long timerResetValue = System.currentTimeMillis() + interval;

        response.setReqId(request.getReqId());
        response.setTimestamp(System.currentTimeMillis());
        if (!(--retries > -1)) {
            status = Status.FAILURE;
            msg = "The timer call has exceeded the maximum number of retries.";
            getLogger().error(msg);
            response.setStatus(status);
            response.setMessage(msg);
            responsePromise.complete(response);
        }
        else if (promise != null && promise.isCompleted()) {
            try {
                IHttpResponse httpResponse = (IHttpResponse) promise.get();
                int respStatus = httpResponse.getStatusCode();

                if (respStatus == 404 && retryOn404) {
                    timerInterval = timerResetValue; // reset timer
                    promise = run(); // refresh the response
                }
                else if (respStatus == 200) {
                    String azureStatus = getStatus(httpResponse);
                    try {
                        if (desiredState.equalsIgnoreCase(azureStatus)) { // azureStatus could be null
                            response = updateResponseObject(request, response, getJaxbObject(httpResponse));
                            status = Status.COMPLETE;
                            msg = "Success";
                            response.setStatus(status);
                            response.setMessage(msg);
                            responsePromise.complete(response);
                        }
                        else if (isFailedState(azureStatus)) { // allows for short-circuit of failed calls
                            status = Status.FAILURE;
                            msg = "The status of " + azureStatus + " was returned which implies the call failed.";
                            getLogger().error(msg);
                            response.setStatus(status);
                            response.setMessage(msg);
                            responsePromise.complete(response);
                        }
                        else {
                            timerInterval = timerResetValue; // reset timer
                            promise = run(); // refresh the response
                        }
                    }
                    catch (Exception e) {
                        status = Status.FAILURE;
                        msg = "An exception occurred while polling status.  " + e.getMessage();
                        getLogger().error(msg, e);
                        response.setStatus(status);
                        response.setMessage(msg);
                        responsePromise.complete(response);
                    }
                }
                else {
                    status = Status.FAILURE;
                    msg = "Unexpected response status was returned by the API call. Status: " + respStatus;
                    getLogger().error(msg);
                    response.setStatus(status);
                    response.setMessage(msg);
                    responsePromise.complete(response);
                }
            }
            catch (Throwable e) {
                status = Status.FAILURE;
                msg = "An exception occurred while polling status.  " + e.getMessage();
                getLogger().error(msg, e);
                response.setStatus(status);
                response.setMessage(msg);
                responsePromise.complete(response);
            }
        }
        else if (promise == null) {
            status = Status.FAILURE;
            msg = "The HTTP Promise object is null.";
            getLogger().error(msg);
            response.setStatus(status);
            response.setMessage(msg);
            responsePromise.complete(response);
        }
        else {
            timerInterval = timerResetValue; // reset timer
        }

        // make sure this is called at the end so a new JAXB object will be computed next time fired
        clearJaxbObject();

        return timerInterval;
    }

    /**
     * This method will reset the JAXB object to null. This should be done at the end of the timer method so the next run will
     * compute a new object from the HTTP response.
     */
    protected void clearJaxbObject()
    {
        this.jaxbObject = null;
    }

    /**
     * This method will extract the JAXB object from the response. For efficiency, make this a singleton for each run of the
     * timer. Make sure to clear it at the end of the timer.
     *
     * @param httpResponse
     *            HTTP response object
     * @return Object - the JAXB conversion of the XML content
     */
    protected Object getJaxbObject(IHttpResponse httpResponse)
    {
        if (jaxbObject == null) {
            jaxbObject = decode(httpResponse);
        }

        return jaxbObject;
    }

    /**
     * This method will check to see if the status represents a failed value.
     *
     * @param statusValue
     *            Value to check
     * @return boolean - true if the status is one of the values representing failure
     */
    abstract protected boolean isFailedState(String statusValue);
}

