/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.amazonaws.ec2.doc._2013_10_15.DescribeVpcsResponseType;
import com.amazonaws.ec2.doc._2013_10_15.VpcSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.AuthorizeSecurityGroupEgressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.AuthorizeSecurityGroupIngressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.CreateSecurityGroupResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.DeleteSecurityGroupResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.DescribeSecurityGroupsResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpPermissionSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpPermissionType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpRangeItemType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpRangeSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.RevokeSecurityGroupEgressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.RevokeSecurityGroupIngressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.SecurityGroupItemType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.SecurityGroupSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.UserIdGroupPairSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.UserIdGroupPairType;
import com.servicemesh.agility.adapters.core.aws.util.AWSUtil;
import com.servicemesh.agility.adapters.core.aws.util.EC2SecurityGroupOperations;
import com.servicemesh.agility.api.AccessList;
import com.servicemesh.agility.api.AccessListDirection;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Protocol;
import com.servicemesh.agility.distributed.sync.AsyncLock;
import com.servicemesh.core.async.CompletablePromise;
import com.servicemesh.core.async.Promise;
import com.servicemesh.core.async.PromiseFactory;
import com.servicemesh.io.http.QueryParams;

// NOTE: sometimes when one of these tests fails there are security groups left out on amazon that need to be manually
// cleaned up. The name of the group should start with AWS_CORE_testGroup and the description should be "Test Security 
// group created by Integration test for core.aws communications bundle"

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.*", "org.xml.*", "org.w3c.dom.*" })
@SuppressStaticInitializationFor({ "com.servicemesh.agility.distributed.sync.AsyncLock" })
@PrepareForTest({ AsyncLock.class })
public class TestSecurityGroupIntegration
{
    private Credential _cred;
    private AWSEndpoint _endpoint;
    private final String EC2_DESCRIBE_VPCS = "DescribeVpcs";
    private final String EC2_VERSION = "2013-10-15";

    @Before
    public void before()
    {
        // Only run these tests if AWS keys have been provided
        _cred = TestHelpers.getAWSCredential();
        Assume.assumeTrue(_cred != null);
        TestHelpers.initLogger(Level.TRACE);
    }

    @Test
    public void testSecurityGroupsConnection() throws Exception
    {
        String address = "https://ec2.us-east-1.amazonaws.com/";

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();

        QueryParams params = conn.initQueryParams(EC2SecurityGroupOperations.EC2_DESCRIBE_SECURITY_GROUPS);
        Promise<DescribeSecurityGroupsResponseType> promise = conn.execute(params, DescribeSecurityGroupsResponseType.class);

        TestHelpers.completePromise(promise, _endpoint, true);
    }

    @Test
    public void testDescribeSecurityGroups() throws Exception
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        String address = "https://ec2.us-east-1.amazonaws.com/";

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);

        QueryParams params = conn.initQueryParams(EC2SecurityGroupOperations.EC2_DESCRIBE_SECURITY_GROUPS);
        Promise<DescribeSecurityGroupsResponseType> promiseToFindGroups =
                conn.execute(params, DescribeSecurityGroupsResponseType.class);
        DescribeSecurityGroupsResponseType resp = null;
        LinkedList<String> groupIds = new LinkedList<String>();
        try {
            resp = promiseToFindGroups.get();
        }
        catch (Throwable e1) {
            throw new Exception(e1);
        }
        assertNotNull(resp);
        SecurityGroupSetType grps = resp.getSecurityGroupInfo();
        List<SecurityGroupItemType> grpItems = grps.getItem();
        for (SecurityGroupItemType item : grpItems) {
            groupIds.add(item.getGroupId());
        }

        assertTrue(!groupIds.isEmpty());

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);

        Promise<SecurityGroupItemType> groupPromise = ops.getSecurityGroup(groupIds.get(0));
        SecurityGroupItemType grp;
        try {
            grp = groupPromise.get();
        }
        catch (Throwable e1) {
            throw new Exception(e1);
        }
        assertNotNull(grp);
        assertEquals(groupIds.get(0), grp.getGroupId());

        Promise<List<SecurityGroupItemType>> groupsPromise = ops.getSecurityGroups(groupIds);
        List<SecurityGroupItemType> groups;
        try {
            groups = groupsPromise.get();
        }
        catch (Throwable e) {
            throw new Exception(e);
        }
        assertNotNull(groups);
        assertEquals(groupIds.size(), groups.size());
    }

    @Test
    public void testCreateSecurityGroups() throws Exception
    {
        String address = "https://ec2.amazonaws.com/";

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);
        AccessList acl = new AccessList();
        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup"));
        acl.setDescription("Test Security group created by Integration test for core.aws communications bundle");
        AccessList acl2 = new AccessList();
        acl2.setName(TestHelpers.generateTestName("AWS_CORE_testGroup2"));
        LinkedList<AccessList> acls = new LinkedList<AccessList>();
        acls.add(acl);
        acls.add(acl2);
        Promise<List<CreateSecurityGroupResponseType>> createPromise = ops.createSecurityGroups(acls, null);
        List<CreateSecurityGroupResponseType> groups = null;
        try {
            groups = createPromise.get();
        }
        catch (Throwable e) {
            throw new Exception(e);
        }
        assertEquals(2, groups.size());
        for (CreateSecurityGroupResponseType rsp : groups) {
            assertTrue(rsp.isReturn());
        }

        //cleanup
        for (CreateSecurityGroupResponseType group : groups) {
            Promise<DeleteSecurityGroupResponseType> deletePromise = ops.deleteSecurityGroupById(group.getGroupId());
            try {
                DeleteSecurityGroupResponseType resp = deletePromise.get();
                assertNotNull(resp);
                assertTrue(resp.isReturn());
            }
            catch (Throwable e) {
                throw new Exception(e);
            }
        }
    }

    @Test
    public void testSecurityGroupCRUD() throws Throwable
    {
        StringBuilder err = new StringBuilder();
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        String address = "https://ec2.us-east-1.amazonaws.com/";

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);
        String sg_id = null;
        AccessList acl = new AccessList();
        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup"));
        acl.setDescription("Test Security group created by Integration test for core.aws communications bundle");
        Promise<CreateSecurityGroupResponseType> createPromise = ops.createSecurityGroup(acl, null);
        CreateSecurityGroupResponseType sg = null;
        try {
            sg = createPromise.get();
            assertTrue(sg.isReturn());
            sg_id = sg.getGroupId();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        Promise<SecurityGroupItemType> getPromise = ops.getSecurityGroup(sg_id);
        SecurityGroupItemType group = null;
        try {
            group = getPromise.get();
            assertEquals(acl.getName(), group.getGroupName());
            assertTrue(group.getIpPermissions().getItem().isEmpty());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol = new Protocol();
        protocol.setAllowed(true);
        protocol.getPrefixes().add("1.1.1.1/32");
        protocol.setProtocol("TCP");
        protocol.setMinPort(1);
        protocol.setMaxPort(2);
        acl.getProtocols().add(protocol);
        acl.setDirection(AccessListDirection.INPUT);
        Promise<Boolean> updatePromise = ops.updateSecurityGroup(acl, group);
        Boolean updateSuccess;
        try {
            updateSuccess = updatePromise.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group update.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        SecurityGroupItemType sg_postUpdate = null;
        try {
            sg_postUpdate = getPromise.get();
            IpPermissionType ipPerm = sg_postUpdate.getIpPermissions().getItem().get(0);
            assertEquals(protocol.getMinPort(), ipPerm.getFromPort());
            assertEquals(protocol.getMaxPort(), ipPerm.getToPort());
            assertTrue(protocol.getProtocol().equalsIgnoreCase(ipPerm.getIpProtocol()));
            assertEquals(protocol.getPrefixes().get(0), ipPerm.getIpRanges().getItem().get(0).getCidrIp());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol2 = new Protocol();
        protocol2.setAllowed(true);
        protocol2.getPrefixes().add("1.1.1.1/32");
        protocol2.setProtocol("TCP");
        protocol2.setMinPort(10);
        protocol2.setMaxPort(11);
        acl.getProtocols().clear();
        acl.getProtocols().add(protocol2);
        Promise<Boolean> updatePromise2 = ops.updateSecurityGroup(acl, sg_postUpdate);
        try {
            updateSuccess = updatePromise2.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        try {
            sg_postUpdate = getPromise.get();
            assertFalse(sg_postUpdate.getIpPermissions().getItem().isEmpty());
            assertEquals(1, sg_postUpdate.getIpPermissions().getItem().size());
            IpPermissionType perm = sg_postUpdate.getIpPermissions().getItem().get(0);
            assertEquals(protocol2.getMinPort(), perm.getFromPort());
            assertEquals(protocol2.getMaxPort(), perm.getToPort());
            assertTrue(protocol2.getProtocol().equalsIgnoreCase(perm.getIpProtocol()));
            assertEquals(protocol2.getPrefixes().get(0), perm.getIpRanges().getItem().get(0).getCidrIp());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol3 = new Protocol();
        protocol3.setAllowed(false);
        protocol3.getPrefixes().add("1.1.1.2/32");
        protocol3.setProtocol("tcp");
        protocol3.setMinPort(5);
        protocol3.setMaxPort(6);
        acl.getProtocols().add(protocol3);
        Promise<Boolean> updatePromise3 = ops.updateSecurityGroup(acl, sg_postUpdate);
        try {
            updateSuccess = updatePromise3.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        try {
            sg_postUpdate = getPromise.get();
            assertFalse(sg_postUpdate.getIpPermissions().getItem().isEmpty());
            assertEquals(1, sg_postUpdate.getIpPermissions().getItem().size());
            IpPermissionType ipType = sg_postUpdate.getIpPermissions().getItem().get(0);
            assertEquals(protocol2.getMinPort(), ipType.getFromPort());
            assertEquals(protocol2.getMaxPort(), ipType.getToPort());
            assertTrue(protocol2.getProtocol().equalsIgnoreCase(ipType.getIpProtocol()));
            assertEquals(protocol2.getPrefixes().get(0), ipType.getIpRanges().getItem().get(0).getCidrIp());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        //cleanup
        Promise<DeleteSecurityGroupResponseType> deletePromise = ops.deleteSecurityGroup(sg_postUpdate);
        try {
            DeleteSecurityGroupResponseType resp = deletePromise.get();
            assertNotNull(resp);
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        if (AWSUtil.isValued(err.toString())) {
            Assert.fail(err.toString());
        }
    }

    @Test
    public void testVPCSecurityGroupCRUD() throws Throwable
    {
        StringBuilder err = new StringBuilder();
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        String address = "https://ec2.us-east-1.amazonaws.com/";

        // find a vpc to use
        AWSEndpoint vpcEndpoint =
                AWSEndpointFactory.getInstance().getEndpoint(address, EC2_VERSION, DescribeVpcsResponseType.class);
        AWSConnection vpcConn = AWSConnectionFactory.getInstance().getConnection(null, _cred, null, vpcEndpoint);

        QueryParams params = vpcConn.initQueryParams(EC2_DESCRIBE_VPCS);
        Promise<DescribeVpcsResponseType> vpcsPromise = vpcConn.execute(params, DescribeVpcsResponseType.class);
        DescribeVpcsResponseType vpcsResp = vpcsPromise.get();
        VpcSetType vpcs = vpcsResp.getVpcSet();
        String vpc_id = vpcs.getItem().get(0).getVpcId();

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);
        String sg_id = null;
        AccessList acl = new AccessList();
        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup"));
        acl.setDescription("Test Security group created by Integration test for core.aws communications bundle");
        acl.setDirection(AccessListDirection.OUTPUT);
        Promise<CreateSecurityGroupResponseType> createPromise = ops.createSecurityGroup(acl, vpc_id);
        CreateSecurityGroupResponseType sg = null;
        try {
            sg = createPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        assertTrue(sg.isReturn());
        sg_id = sg.getGroupId();
        Thread.sleep(500);

        Promise<SecurityGroupItemType> getPromise = ops.getSecurityGroup(sg_id);
        SecurityGroupItemType group = null;
        try {
            group = getPromise.get();
            assertEquals(acl.getName(), group.getGroupName());
            assertTrue(group.getIpPermissions().getItem().isEmpty());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol = new Protocol();
        protocol.setAllowed(true);
        protocol.getPrefixes().add("1.1.1.1/32");
        protocol.setProtocol("TCP");
        protocol.setMinPort(1);
        protocol.setMaxPort(2);
        acl.getProtocols().add(protocol);
        acl.setDirection(AccessListDirection.OUTPUT);
        Promise<Boolean> updatePromise = ops.updateSecurityGroup(acl, group);
        Boolean updateSuccess;
        try {
            updateSuccess = updatePromise.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group update.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        SecurityGroupItemType sg_postUpdate = null;
        try {
            sg_postUpdate = getPromise.get();
            assertFalse(sg_postUpdate.getIpPermissionsEgress().getItem().isEmpty());
            IpPermissionType ipPerm = sg_postUpdate.getIpPermissionsEgress().getItem().get(0);
            assertEquals(protocol.getMinPort(), ipPerm.getFromPort());
            assertEquals(protocol.getMaxPort(), ipPerm.getToPort());
            assertTrue(protocol.getProtocol().equalsIgnoreCase(ipPerm.getIpProtocol()));
            assertEquals(protocol.getPrefixes().get(0), ipPerm.getIpRanges().getItem().get(0).getCidrIp());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol2 = new Protocol();
        protocol2.setAllowed(true);
        protocol2.getPrefixes().add("1.1.1.1/32");
        protocol2.setProtocol("TCP");
        protocol2.setMinPort(10);
        protocol2.setMaxPort(11);
        acl.getProtocols().clear();
        acl.getProtocols().add(protocol2);
        Promise<Boolean> updatePromise2 = ops.updateSecurityGroup(acl, sg_postUpdate);
        try {
            updateSuccess = updatePromise2.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group update.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        try {
            sg_postUpdate = getPromise.get();
            assertFalse(sg_postUpdate.getIpPermissionsEgress().getItem().isEmpty());
            assertEquals(2, sg_postUpdate.getIpPermissionsEgress().getItem().size());
            for (IpPermissionType perm : sg_postUpdate.getIpPermissionsEgress().getItem()) {
                if (perm.getToPort() == null && perm.getFromPort() == null && perm.getIpProtocol().equals("-1")
                        && perm.getIpRanges().getItem().get(0).getCidrIp().equals("0.0.0.0/0")) {
                    continue; //skip the default rule
                }
                assertEquals(protocol2.getMinPort(), perm.getFromPort());
                assertEquals(protocol2.getMaxPort(), perm.getToPort());
                assertTrue(protocol2.getProtocol().equalsIgnoreCase(perm.getIpProtocol()));
                assertEquals(protocol2.getPrefixes().get(0), perm.getIpRanges().getItem().get(0).getCidrIp());
            }
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        Protocol protocol3 = new Protocol();
        protocol3.setAllowed(false);
        protocol3.getPrefixes().add("1.1.1.2/32");
        protocol3.setProtocol("TCP");
        protocol3.setMinPort(5);
        protocol3.setMaxPort(6);
        acl.getProtocols().add(protocol3);
        Promise<Boolean> updatePromise3 = ops.updateSecurityGroup(acl, sg_postUpdate);
        try {
            updateSuccess = updatePromise3.get();
            assertTrue(updateSuccess);
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group update.\n");
            err.append(e + "\n\n");
        }
        Thread.sleep(500);

        getPromise = ops.getSecurityGroup(sg_id);
        try {
            sg_postUpdate = getPromise.get();
            assertFalse(sg_postUpdate.getIpPermissionsEgress().getItem().isEmpty());
            assertEquals(2, sg_postUpdate.getIpPermissionsEgress().getItem().size());
            for (IpPermissionType ipType : sg_postUpdate.getIpPermissionsEgress().getItem()) {
                if (ipType.getToPort() == null && ipType.getFromPort() == null && ipType.getIpProtocol().equals("-1")
                        && ipType.getIpRanges().getItem().get(0).getCidrIp().equals("0.0.0.0/0")) {
                    continue; //skip the default rule
                }
                assertEquals(protocol2.getMinPort(), ipType.getFromPort());
                assertEquals(protocol2.getMaxPort(), ipType.getToPort());
                assertTrue(protocol2.getProtocol().equalsIgnoreCase(ipType.getIpProtocol()));
                assertEquals(protocol2.getPrefixes().get(0), ipType.getIpRanges().getItem().get(0).getCidrIp());
            }
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        //cleanup
        Promise<DeleteSecurityGroupResponseType> deletePromise = ops.deleteSecurityGroup(sg_postUpdate);
        DeleteSecurityGroupResponseType resp;
        try {
            resp = deletePromise.get();
            assertNotNull(resp);
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group delete.\n");
            err.append(e + "\n\n");
        }

        if (AWSUtil.isValued(err.toString())) {
            Assert.fail(err.toString());
        }
    }

    @Test
    public void testIsDefaultRule() throws Exception
    {
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(null);
        IpPermissionType perm = new IpPermissionType();
        String CidrIp = "0.0.0.0/0";
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", (IpPermissionType) null, CidrIp));
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
        perm.setToPort(1);
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
        perm.setToPort(null);
        perm.setFromPort(1);
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
        perm.setIpProtocol("tcp");
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
        perm.setFromPort(null);
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
        perm.setIpProtocol("-1");
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, (String) null));
        assertFalse((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, "fail"));
        assertTrue((Boolean) Whitebox.invokeMethod(ops, "isDefaultRule", perm, CidrIp));
    }

    @Test
    public void testAuthorizeIngressExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> addPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(addPromise, cancelPromise);
        addPromise.failure(new Exception("Test Failure"));

        IpPermissionType perm = new IpPermissionType();
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<AuthorizeSecurityGroupIngressResponseType> promise = ops.authorizeSecurityGroupIngress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.authorizeSecurityGroupIngress("id", perm);
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.authorizeSecurityGroupIngress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testAuthorizeEgressExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> addPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(addPromise, cancelPromise);
        addPromise.failure(new Exception("Test Failure"));

        IpPermissionType perm = new IpPermissionType();
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<AuthorizeSecurityGroupEgressResponseType> promise = ops.authorizeSecurityGroupEgress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.authorizeSecurityGroupEgress("id", perm);
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.authorizeSecurityGroupEgress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testRevokeIngressExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> removePromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(removePromise, cancelPromise);
        removePromise.failure(new Exception("Test Failure"));

        IpPermissionType perm = new IpPermissionType();
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<RevokeSecurityGroupIngressResponseType> promise = ops.revokeSecurityGroupIngress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.revokeSecurityGroupIngress("id", perm);
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.revokeSecurityGroupIngress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testRevokeEgressExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> removePromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(removePromise, cancelPromise);
        removePromise.failure(new Exception("Test Failure"));

        IpPermissionType perm = new IpPermissionType();
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<RevokeSecurityGroupEgressResponseType> promise = ops.revokeSecurityGroupEgress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.revokeSecurityGroupEgress("id", perm);
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.revokeSecurityGroupEgress("id", perm);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testGetGroupExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> getPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(getPromise, cancelPromise);
        getPromise.failure(new Exception("Test Failure"));

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<SecurityGroupItemType> promise = ops.getSecurityGroup("id");
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.getSecurityGroup("id");
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.getSecurityGroup("id");
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testGetGroupsExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> getPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(getPromise, cancelPromise);
        getPromise.failure(new Exception("Test Failure"));

        LinkedList<String> ids = new LinkedList<String>();
        ids.add("id");
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<List<SecurityGroupItemType>> promise = ops.getSecurityGroups(ids);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.getSecurityGroups(ids);
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.getSecurityGroups(ids);
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testDeleteGroupExceptions()
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> delPromise = PromiseFactory.create();
        CompletablePromise<AuthorizeSecurityGroupIngressResponseType> cancelPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(delPromise, cancelPromise);
        delPromise.failure(new Exception("Test Failure"));

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        Promise<DeleteSecurityGroupResponseType> promise = ops.deleteSecurityGroupById("id");
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Failure"));
        }

        promise = ops.deleteSecurityGroupById("id");
        try {
            promise.cancel();
        }
        catch (Throwable e) {
            //ignoring illegal state exception. Just want to trigger onCancel.
        }

        when(mockConn.initQueryParams(any(String.class))).thenThrow(new RuntimeException("Test Exception"));
        promise = ops.deleteSecurityGroupById("id");
        try {
            promise.get();
            Assert.fail("Shouldn't reach here");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("Test Exception"));
        }
    }

    @Test
    public void testUpdateDoNothing() throws Throwable
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        CompletablePromise<DescribeSecurityGroupsResponseType> getPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());
        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(getPromise);
        DescribeSecurityGroupsResponseType grp = new DescribeSecurityGroupsResponseType();
        SecurityGroupSetType sg = new SecurityGroupSetType();
        sg.getItem().add(new SecurityGroupItemType());
        grp.setSecurityGroupInfo(sg);
        getPromise.complete(grp);

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);
        AccessList acl = new AccessList();
        SecurityGroupItemType item = new SecurityGroupItemType();
        Promise<Boolean> promise = ops.updateSecurityGroup(acl, item);
        try {
            promise.get();
        }
        catch (Exception e) {
            //ignore
        }
        acl.setDirection(AccessListDirection.OUTPUT);
        promise = ops.updateSecurityGroup(acl, item);
        try {
            promise.get();
        }
        catch (Exception e) {
            //ignore
        }
    }

    @Test
    public void testUpdateFailures() throws Throwable
    {
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        AWSConnection mockConn = mock(AWSConnection.class);
        when(mockConn.initQueryParams(any(String.class))).thenReturn(new QueryParams());

        AuthorizeSecurityGroupIngressResponseType authIn = new AuthorizeSecurityGroupIngressResponseType();
        authIn.setReturn(false);
        Promise<AuthorizeSecurityGroupIngressResponseType> authInPromise = Promise.pure(authIn);

        RevokeSecurityGroupIngressResponseType revokeIn = new RevokeSecurityGroupIngressResponseType();
        revokeIn.setReturn(false);
        Promise<RevokeSecurityGroupIngressResponseType> revokeInPromise = Promise.pure(revokeIn);

        AuthorizeSecurityGroupEgressResponseType authE = new AuthorizeSecurityGroupEgressResponseType();
        authE.setReturn(false);
        Promise<AuthorizeSecurityGroupEgressResponseType> authEPromise = Promise.pure(authE);

        RevokeSecurityGroupEgressResponseType revokeE = new RevokeSecurityGroupEgressResponseType();
        revokeE.setReturn(false);
        Promise<RevokeSecurityGroupEgressResponseType> revokeEPromise = Promise.pure(revokeE);

        when(mockConn.execute(any(QueryParams.class), any(Class.class))).thenReturn(authInPromise, authEPromise, revokeInPromise,
                revokeEPromise);

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        AccessList acl = new AccessList();
        Protocol prot = new Protocol();
        prot.setAllowed(true);
        prot.getPrefixes().add("pre");
        prot.setProtocol("tcp");
        acl.getProtocols().add(prot);
        SecurityGroupItemType item = new SecurityGroupItemType();
        item.setGroupId("id");
        Promise<Boolean> promise = ops.updateSecurityGroup(acl, item);
        try {
            assertFalse(promise.get());
        }
        catch (Throwable e) {
            throw e;
        }

        acl.setDirection(AccessListDirection.OUTPUT);
        promise = ops.updateSecurityGroup(acl, item);
        try {
            assertFalse(promise.get());
        }
        catch (Throwable e) {
            throw e;
        }

        acl.getProtocols().clear();
        IpRangeItemType range = new IpRangeItemType();
        range.setCidrIp("ip");
        IpRangeSetType ranges = new IpRangeSetType();
        ranges.getItem().add(range);
        IpPermissionType perm = new IpPermissionType();
        perm.setIpRanges(ranges);
        IpPermissionSetType perms = new IpPermissionSetType();
        perms.getItem().add(perm);
        item.setIpPermissions(perms);
        acl.setDirection(null);
        promise = ops.updateSecurityGroup(acl, item);
        try {
            assertFalse(promise.get());
        }
        catch (Throwable e) {
            throw e;
        }

        acl.setDirection(AccessListDirection.OUTPUT);
        item.setIpPermissionsEgress(perms);
        promise = ops.updateSecurityGroup(acl, item);
        try {
            assertFalse(promise.get());
        }
        catch (Throwable e) {
            throw e;
        }

    }

    @Test
    public void testAuthorizeRevokeMissingParams()
    {
        AWSConnection mockConn = mock(AWSConnection.class);
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        Promise<AuthorizeSecurityGroupIngressResponseType> promise = ops.authorizeSecurityGroupIngress(null, null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }

        promise = ops.authorizeSecurityGroupIngress("id", null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("perm"));
        }

        Promise<AuthorizeSecurityGroupEgressResponseType> promise2 = ops.authorizeSecurityGroupEgress(null, null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }

        promise2 = ops.authorizeSecurityGroupEgress("id", null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("perm"));
        }

        Promise<RevokeSecurityGroupIngressResponseType> promise3 = ops.revokeSecurityGroupIngress(null, null);
        try {
            promise3.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }

        promise3 = ops.revokeSecurityGroupIngress("id", null);
        try {
            promise3.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("perm"));
        }

        Promise<RevokeSecurityGroupEgressResponseType> promise4 = ops.revokeSecurityGroupEgress(null, null);
        try {
            promise4.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }

        promise4 = ops.revokeSecurityGroupEgress("id", null);
        try {
            promise4.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("perm"));
        }
    }

    @Test
    public void testCreatesMissingParams()
    {
        AWSConnection mockConn = mock(AWSConnection.class);
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        Promise<List<CreateSecurityGroupResponseType>> promise = ops.createSecurityGroups(null, null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("access lists"));
        }

        Promise<CreateSecurityGroupResponseType> promise2 = ops.createSecurityGroup(null, null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("access list"));
        }

        Promise<CreateSecurityGroupResponseType> promise3 = ops.createSecurityGroup(null, null, null);
        try {
            promise3.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("name"));
        }
    }

    @Test
    public void testGetsMissingParams()
    {
        AWSConnection mockConn = mock(AWSConnection.class);
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        Promise<List<SecurityGroupItemType>> promise = ops.getSecurityGroups(null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupIds"));
        }

        Promise<SecurityGroupItemType> promise2 = ops.getSecurityGroup(null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }
    }

    @Test
    public void testDeletesMissingParams()
    {
        AWSConnection mockConn = mock(AWSConnection.class);
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        Promise<DeleteSecurityGroupResponseType> promise = ops.deleteSecurityGroup(null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("security group"));
        }

        Promise<DeleteSecurityGroupResponseType> promise2 = ops.deleteSecurityGroupById(null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("groupId"));
        }
    }

    @Test
    public void testUpdateMissingParams()
    {
        AWSConnection mockConn = mock(AWSConnection.class);
        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(mockConn);

        Promise<Boolean> promise = ops.updateSecurityGroup(null, null);
        try {
            promise.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("access list"));
        }

        Promise<Boolean> promise2 = ops.updateSecurityGroup(new AccessList(), null);
        try {
            promise2.get();
            Assert.fail("Shouldn't get here.");
        }
        catch (Throwable e) {
            assertNotNull(e);
            assertTrue(e.getMessage().contains("security group"));
        }
    }

    @Test
    public void testIngress() throws Throwable
    {
        StringBuilder err = new StringBuilder();
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        String address = "https://ec2.us-east-1.amazonaws.com/";

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);
        AccessList acl = new AccessList();
        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup"));
        acl.setDescription("Test Security group created by Integration test for core.aws communications bundle");
        Promise<CreateSecurityGroupResponseType> createPromise = ops.createSecurityGroup(acl, null);
        CreateSecurityGroupResponseType sg = null;
        try {
            sg = createPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        assertTrue(sg.isReturn());
        String sg_id = sg.getGroupId();

        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup1"));
        createPromise = ops.createSecurityGroup(acl, null);
        try {
            sg = createPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        assertTrue(sg.isReturn());
        String child_id = sg.getGroupId();

        IpPermissionType perm = new IpPermissionType();
        perm.setIpRanges(new IpRangeSetType());
        perm.setGroups(new UserIdGroupPairSetType());
        Promise<AuthorizeSecurityGroupIngressResponseType> authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
            assertTrue(t.getMessage().contains("400"));
        }

        perm.setIpRanges(null);
        UserIdGroupPairSetType groups = new UserIdGroupPairSetType();
        UserIdGroupPairType group = new UserIdGroupPairType();
        group.setGroupId(child_id);
        groups.getItem().add(group);
        perm.setGroups(groups);
        authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
            assertTrue(t.getMessage().contains("400"));
        }

        Promise<SecurityGroupItemType> getPromise = ops.getSecurityGroup(child_id);
        SecurityGroupItemType childGroup = null;
        try {
            childGroup = getPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        group.setUserId(childGroup.getOwnerId());
        authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
        }

        group.setGroupId(null);
        group.setGroupName(childGroup.getGroupName());
        perm.setIpRanges(new IpRangeSetType());
        authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        AuthorizeSecurityGroupIngressResponseType resp = null;
        try {
            resp = authPromise.get();
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group auth ingress.\n");
            err.append(e + "\n\n");
        }

        Promise<RevokeSecurityGroupIngressResponseType> revoke = ops.revokeSecurityGroupIngress(sg_id, perm);
        RevokeSecurityGroupIngressResponseType revokeResp = null;
        try {
            revokeResp = revoke.get();
            assertTrue(revokeResp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group revoke ingress.\n");
            err.append(e + "\n\n");
        }

        perm.setIpProtocol("tcp");
        perm.setFromPort(1);
        perm.setToPort(2);
        authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        try {
            resp = authPromise.get();
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group auth ingress.\n");
            err.append(e + "\n\n");
        }

        revoke = ops.revokeSecurityGroupIngress(sg_id, perm);
        try {
            revokeResp = revoke.get();
            assertTrue(revokeResp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group revoke ingress.\n");
            err.append(e + "\n\n");
        }

        group.setGroupId(child_id);
        group.setGroupName(null);
        group.setUserId(null);
        perm.setFromPort(3);
        perm.setToPort(4);
        authPromise = ops.authorizeSecurityGroupIngress(sg_id, perm);
        try {
            resp = authPromise.get();
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group auth ingress.\n");
            err.append(e + "\n\n");
        }

        revoke = ops.revokeSecurityGroupIngress(sg_id, perm);
        try {
            revokeResp = revoke.get();
            assertTrue(revokeResp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group revoke ingress.\n");
            err.append(e + "\n\n");
        }

        //cleanup
        Promise<DeleteSecurityGroupResponseType> deletePromise = ops.deleteSecurityGroupById(sg_id);
        DeleteSecurityGroupResponseType del = null;
        try {
            del = deletePromise.get();
            assertNotNull(del);
            assertTrue(del.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group delete.\n");
            err.append(e + "\n\n");
        }

        deletePromise = ops.deleteSecurityGroupById(child_id);
        try {
            del = deletePromise.get();
            assertNotNull(del);
            assertTrue(del.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group delete.\n");
            err.append(e + "\n\n");
        }

        if (AWSUtil.isValued(err.toString())) {
            Assert.fail(err.toString());
        }
    }

    @Test
    public void testEgress() throws Throwable
    {
        StringBuilder err = new StringBuilder();
        PowerMockito.mockStatic(AsyncLock.class);
        AsyncLock mockLock = mock(AsyncLock.class);
        CompletablePromise<AsyncLock> lockPromise = PromiseFactory.create();
        when(AsyncLock.lock(any(String.class))).thenReturn(lockPromise);
        lockPromise.complete(mockLock);
        String address = "https://ec2.us-east-1.amazonaws.com/";

        // find a vpc to use
        AWSEndpoint vpcEndpoint =
                AWSEndpointFactory.getInstance().getEndpoint(address, EC2_VERSION, DescribeVpcsResponseType.class);
        AWSConnection vpcConn = AWSConnectionFactory.getInstance().getConnection(null, _cred, null, vpcEndpoint);

        QueryParams params = vpcConn.initQueryParams(EC2_DESCRIBE_VPCS);
        Promise<DescribeVpcsResponseType> vpcsPromise = vpcConn.execute(params, DescribeVpcsResponseType.class);
        DescribeVpcsResponseType vpcsResp = vpcsPromise.get();
        VpcSetType vpcs = vpcsResp.getVpcSet();
        String vpc_id = vpcs.getItem().get(0).getVpcId();

        AWSConnection conn = AWSConnectionFactory.getInstance().getSecurityGroupConnection(null, _cred, null, address);
        _endpoint = conn.getEndpoint();

        EC2SecurityGroupOperations ops = new EC2SecurityGroupOperations(conn);
        AccessList acl = new AccessList();
        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup"));
        acl.setDescription("Test Security group created by Integration test for core.aws communications bundle");
        Promise<CreateSecurityGroupResponseType> createPromise = ops.createSecurityGroup(acl, vpc_id);
        CreateSecurityGroupResponseType sg = null;
        try {
            sg = createPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        assertTrue(sg.isReturn());
        String sg_id = sg.getGroupId();

        acl.setName(TestHelpers.generateTestName("AWS_CORE_testGroup1"));
        createPromise = ops.createSecurityGroup(acl, vpc_id);
        try {
            sg = createPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group create.\n");
            err.append(e + "\n\n");
        }
        assertTrue(sg.isReturn());
        String child_id = sg.getGroupId();

        IpPermissionType perm = new IpPermissionType();
        perm.setIpRanges(new IpRangeSetType());
        perm.setGroups(new UserIdGroupPairSetType());
        Promise<AuthorizeSecurityGroupEgressResponseType> authPromise = ops.authorizeSecurityGroupEgress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
            assertTrue(t.getMessage().contains("400"));
        }

        perm.setIpRanges(null);
        UserIdGroupPairSetType groups = new UserIdGroupPairSetType();
        UserIdGroupPairType group = new UserIdGroupPairType();
        group.setGroupId(child_id);
        groups.getItem().add(group);
        perm.setGroups(groups);
        authPromise = ops.authorizeSecurityGroupEgress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
            assertTrue(t.getMessage().contains("400"));
        }

        Promise<SecurityGroupItemType> getPromise = ops.getSecurityGroup(child_id);
        SecurityGroupItemType childGroup = null;
        try {
            childGroup = getPromise.get();
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group get.\n");
            err.append(e + "\n\n");
        }

        group.setGroupId(null);
        group.setUserId(childGroup.getOwnerId());
        perm.setIpRanges(new IpRangeSetType());
        perm.setIpProtocol("tcp");
        perm.setFromPort(1);
        perm.setToPort(2);
        authPromise = ops.authorizeSecurityGroupEgress(sg_id, perm);
        try {
            authPromise.get();
            Assert.fail("Shouldn't get here");
        }
        catch (Throwable t) {
            assertNotNull(t);
        }

        group.setGroupId(child_id);
        group.setGroupName(childGroup.getGroupName());
        authPromise = ops.authorizeSecurityGroupEgress(sg_id, perm);
        AuthorizeSecurityGroupEgressResponseType resp = null;
        try {
            resp = authPromise.get();
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group auth egress.\n");
            err.append(e + "\n\n");
        }

        Promise<RevokeSecurityGroupEgressResponseType> revoke = ops.revokeSecurityGroupEgress(sg_id, perm);
        RevokeSecurityGroupEgressResponseType revokeResp = null;
        try {
            revokeResp = revoke.get();
            assertTrue(revokeResp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group revoke egress.\n");
            err.append(e + "\n\n");
        }

        group.setGroupId(child_id);
        group.setGroupName(null);
        group.setUserId(null);
        perm.setFromPort(3);
        perm.setToPort(4);
        authPromise = ops.authorizeSecurityGroupEgress(sg_id, perm);
        try {
            resp = authPromise.get();
            assertTrue(resp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group auth egress.\n");
            err.append(e + "\n\n");
        }

        revoke = ops.revokeSecurityGroupEgress(sg_id, perm);
        try {
            revokeResp = revoke.get();
            assertTrue(revokeResp.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group revoke egress.\n");
            err.append(e + "\n\n");
        }

        //cleanup
        Promise<DeleteSecurityGroupResponseType> deletePromise = ops.deleteSecurityGroupById(sg_id);
        DeleteSecurityGroupResponseType del = null;
        try {
            del = deletePromise.get();
            assertNotNull(del);
            assertTrue(del.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group delete.\n");
            err.append(e + "\n\n");
        }

        deletePromise = ops.deleteSecurityGroupById(child_id);
        try {
            del = deletePromise.get();
            assertNotNull(del);
            assertTrue(del.isReturn());
        }
        catch (Exception e) {
            System.out.println(e);
            err.append("Exception during security group delete.\n");
            err.append(e + "\n\n");
        }

        if (AWSUtil.isValued(err.toString())) {
            Assert.fail(err.toString());
        }
    }
}
