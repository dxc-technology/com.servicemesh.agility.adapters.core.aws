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
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpResponse;
import com.servicemesh.io.http.QueryParam;
import com.servicemesh.io.http.QueryParams;
import com.servicemesh.io.proxy.Proxy;

import com.amazonaws.rds.doc._2010_07_28.CreateDBParameterGroup;
import com.amazonaws.rds.doc._2010_07_28.CreateDBParameterGroupResponse;
import com.amazonaws.rds.doc._2010_07_28.CreateDBParameterGroupResult;
import com.amazonaws.rds.doc._2010_07_28.DBParameterGroup;
import com.amazonaws.rds.doc._2010_07_28.DescribeDBParametersResponse;
import com.amazonaws.rds.doc._2010_07_28.DescribeDBParametersResult;
import com.amazonaws.rds.doc._2010_07_28.ModifyDBParameterGroupResponse;
import com.amazonaws.rds.doc._2010_07_28.ModifyDBParameterGroupResult;
import com.amazonaws.rds.doc._2010_07_28.Parameter;
import com.amazonaws.rds.doc._2010_07_28.ParametersList;
import com.amazonaws.rds.doc._2010_07_28.ResponseMetadata;

/** AWS Relational Database Service integration tests */
public class TestRDSIntegration
{
    private static final String RDS_DBPG_FAMILY = "MySQL5.1";
    private static final String RDS_DBPG_NAME = "SmfyTestAWSIntegration";

    // Include special characters to test RFC 3986 compliance
    private static final String RDS_DBPG_DESCRIBE =
        "Integration Test for core.aws: RFC 3986 START + * ~ - _ . & ! END";
    private static final String RDS_DBPG_PARM_NAME = "binlog_cache_size";
    private static final String RDS_DBPG_PARM_VALUE = "65536";

    private Credential _cred;
    private List<Property> _settings = null;
    private Proxy _proxy = null;
    private AWSEndpoint _endpoint;

    @Before
    public void before()
    {
        // Only run these tests if AWS keys have been provided
        _cred = TestHelpers.getAWSCredential();
        Assume.assumeTrue(_cred != null);
        TestHelpers.initLogger(Level.TRACE);
    }

    @Test
    public void testRDS() throws Exception
    {
        String address = "https://rds.us-west-1.amazonaws.com";
        String version = "2010-07-28";
        _endpoint = AWSEndpointFactory.getInstance()
            .getEndpoint(address, version, CreateDBParameterGroup.class);

        AWSConnection conn = AWSConnectionFactory.getInstance()
            .getConnection(_settings, _cred, _proxy, _endpoint);

        boolean cleanup = true;
        try {
            createRDS(conn);
            updateRDS(conn);
            retrieveRDS(conn);
            deleteRDS(conn);
            cleanup = false;
        }
        finally {
            if (cleanup) {
                try {
                    cleanupRDS(conn);
                }
                catch (Exception e) {}
            }
        }
    }

    private void createRDS(AWSConnection conn) throws Exception
    {
        QueryParams params = conn.initQueryParams("CreateDBParameterGroup");
        params.add(new QueryParam("DBParameterGroupFamily", RDS_DBPG_FAMILY));
        params.add(new QueryParam("DBParameterGroupName", RDS_DBPG_NAME));
        params.add(new QueryParam("Description", RDS_DBPG_DESCRIBE));

        Promise<CreateDBParameterGroupResponse> promise =
            conn.execute(params, CreateDBParameterGroupResponse.class);
        CreateDBParameterGroupResponse cdbpgResponse =
            TestHelpers.completePromise(promise, _endpoint, true);
        ResponseMetadata rm = cdbpgResponse.getResponseMetadata();
        Assert.assertNotNull(rm);
        Assert.assertNotNull(rm.getRequestId());

        CreateDBParameterGroupResult cdbpgResult =
            cdbpgResponse.getCreateDBParameterGroupResult();
        Assert.assertNotNull(cdbpgResult);
        DBParameterGroup dbpg = cdbpgResult.getDBParameterGroup();
        Assert.assertNotNull(dbpg);
        Assert.assertTrue(RDS_DBPG_FAMILY.equalsIgnoreCase(dbpg.getDBParameterGroupFamily()));
        Assert.assertTrue(RDS_DBPG_NAME.equalsIgnoreCase(dbpg.getDBParameterGroupName()));
        Assert.assertEquals(RDS_DBPG_DESCRIBE, dbpg.getDescription());
    }

    private void updateRDS(AWSConnection conn) throws Exception
    {
        QueryParams params = conn.initQueryParams("ModifyDBParameterGroup");
        params.add(new QueryParam("DBParameterGroupName", RDS_DBPG_NAME));

        String baseParam = "Parameters.member.1.";
        params.add(new QueryParam(baseParam + "ParameterName", RDS_DBPG_PARM_NAME));
        params.add(new QueryParam(baseParam + "ParameterValue", RDS_DBPG_PARM_VALUE));
        params.add(new QueryParam(baseParam + "ApplyMethod", "immediate"));

        Promise<ModifyDBParameterGroupResponse> promise =
            conn.execute(params, ModifyDBParameterGroupResponse.class);
        ModifyDBParameterGroupResponse mdbpgResponse =
            TestHelpers.completePromise(promise, _endpoint, true);
        ResponseMetadata rm = mdbpgResponse.getResponseMetadata();
        Assert.assertNotNull(rm);
        Assert.assertNotNull(rm.getRequestId());

        ModifyDBParameterGroupResult mdbpgResult =
            mdbpgResponse.getModifyDBParameterGroupResult();
        Assert.assertNotNull(mdbpgResult);
        Assert.assertTrue(RDS_DBPG_NAME.equalsIgnoreCase(mdbpgResult.getDBParameterGroupName()));
    }

    private void retrieveRDS(AWSConnection conn) throws Exception
    {
        QueryParams params = conn.initQueryParams("DescribeDBParameters");
        params.add(new QueryParam("DBParameterGroupName", RDS_DBPG_NAME));
        params.add(new QueryParam("Source", "user"));

        Promise<DescribeDBParametersResponse> promise =
            conn.execute(params, DescribeDBParametersResponse.class);
        DescribeDBParametersResponse rdbpgResponse =
            TestHelpers.completePromise(promise, _endpoint, true);
        ResponseMetadata rm = rdbpgResponse.getResponseMetadata();
        Assert.assertNotNull(rm);
        Assert.assertNotNull(rm.getRequestId());

        DescribeDBParametersResult rdbpgResult =
            rdbpgResponse.getDescribeDBParametersResult();
        Assert.assertNotNull(rdbpgResult);

        ParametersList parms = rdbpgResult.getParameters();
        Assert.assertNotNull(parms);

        List<Parameter> parmList = parms.getParameter();
        Assert.assertNotNull(parmList);
        Assert.assertTrue(! parmList.isEmpty());

        Parameter parm = parmList.get(0);
        Assert.assertTrue(RDS_DBPG_PARM_NAME.equals(parm.getParameterName()));
        Assert.assertTrue(RDS_DBPG_PARM_VALUE.equals(parm.getParameterValue()));
    }

    private void deleteRDS(AWSConnection conn) throws Exception
    {
        IHttpResponse response = cleanupRDS(conn);
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatusCode());
    }

    private IHttpResponse cleanupRDS(AWSConnection conn) throws Exception
    {
        QueryParams params = conn.initQueryParams("DeleteDBParameterGroup");
        params.add(new QueryParam("DBParameterGroupName", RDS_DBPG_NAME));
        Promise<IHttpResponse> promise = conn.execute(HttpMethod.DELETE, params);
        try {
            return promise.get();
        }
        catch (Throwable t) {
            return null;
        }
    }
}
