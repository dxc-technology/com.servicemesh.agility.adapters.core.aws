/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.servicemesh.agility.adapters.core.aws.AWSConnection;
import com.servicemesh.agility.adapters.core.aws.AWSConnectionFactory;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.AuthorizeSecurityGroupEgressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.AuthorizeSecurityGroupIngressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.CreateSecurityGroupResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.DeleteSecurityGroupResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.DescribeSecurityGroupsResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpPermissionType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpRangeItemType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.IpRangeSetType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.RevokeSecurityGroupEgressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.RevokeSecurityGroupIngressResponseType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.SecurityGroupItemType;
import com.servicemesh.agility.adapters.core.aws.security.group.resources.UserIdGroupPairType;
import com.servicemesh.agility.api.AccessList;
import com.servicemesh.agility.api.AccessListDirection;
import com.servicemesh.agility.api.Protocol;
import com.servicemesh.agility.distributed.sync.AsyncLock;
import com.servicemesh.core.async.Callback;
import com.servicemesh.core.async.Function;
import com.servicemesh.core.async.Promise;
import com.servicemesh.io.http.QueryParam;
import com.servicemesh.io.http.QueryParams;

/**
 * Operation methods for managing EC2 Security Groups.
 */
public class EC2SecurityGroupOperations
{

    private AWSConnection _connection;

    /** "CreateSecurityGroup" action parameter. */
    public static final String EC2_CREATE_SECURITY_GROUP = "CreateSecurityGroup";
    /** "DescribeSecurityGroups" action parameter. */
    public static final String EC2_DESCRIBE_SECURITY_GROUPS = "DescribeSecurityGroups";
    /** "AuthorizeSecurityGroupIngress" action parameter. */
    public static final String EC2_AUTHORIZE_SECURITY_GROUP_INGRESS = "AuthorizeSecurityGroupIngress";
    /** "AuthorizeSecurityGroupEgress" action parameter. */
    public static final String EC2_AUTHORIZE_SECURITY_GROUP_EGRESS = "AuthorizeSecurityGroupEgress";
    /** "RevokeSecurityGroupIngress" action parameter. */
    public static final String EC2_REVOKE_SECURITY_GROUP_INGRESS = "RevokeSecurityGroupIngress";
    /** "RevokeSecurityGroupEgress" action parameter. */
    public static final String EC2_REVOKE_SECURITY_GROUP_EGRESS = "RevokeSecurityGroupEgress";
    /** "DeleteSecurityGroup" action parameter. */
    public static final String EC2_DELETE_SECURITY_GROUP = "DeleteSecurityGroup";
    private static final String EC2_CLASSIC_SECURITY_GROUP_ID = "GroupId";
    private static final String EC2_VPC_SECURITY_GROUP_ID = "GroupId";

    /**
     * Constructor for EC2SecurityGroupOperations.
     * 
     * @param connection
     *            An AWS Connection for EC2. The
     *            {@link AWSConnectionFactory#getSecurityGroupConnection(List, com.servicemesh.agility.api.Credential, com.servicemesh.io.proxy.Proxy, String)}
     *            method can be used to create the connection.
     */
    public EC2SecurityGroupOperations(AWSConnection connection)
    {
        _connection = connection;
    }

    /**
     * Iterates through a list of AccessLists and creates a security group for each one.
     * 
     * @see #createSecurityGroup(AccessList, String)
     * @param acls
     *            List of AccessLists to create security groups for.
     * @param vpcId
     *            For EC2-VPC security groups, the id of the VPC the group(s) are for. Optional, may be null.
     * @return A list of the resulting security groups.
     */
    public Promise<List<CreateSecurityGroupResponseType>> createSecurityGroups(final List<AccessList> acls, String vpcId)
    {
        if (!AWSUtil.isValued(acls)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingAcls")));
        }
        List<Promise<CreateSecurityGroupResponseType>> promises = new ArrayList<Promise<CreateSecurityGroupResponseType>>();
        for (AccessList acl : acls)
            promises.add(createSecurityGroup(acl, vpcId));
        return Promise.sequence(promises);
    }

    /**
     * Creates a security group for the given AccessList. If a vpcId is provided then an EC2-VPC security group is created. The
     * name of the security group is set to the name of the AccessList. The description of the security group is set to the
     * description of the AccessList.
     * 
     * @see #createSecurityGroup(String, String, String)
     * @param acl
     *            AccessList to create a security group for.
     * @param vpcId
     *            For EC2-VPC security groups, the id of the VPC the group is for. Optional, may be null.
     * @return A CreateSecurityGroupResponseType that contains the id of the new group.
     */
    public Promise<CreateSecurityGroupResponseType> createSecurityGroup(AccessList acl, String vpcId)
    {
        if (!AWSUtil.isValued(acl)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingAcl")));
        }
        return createSecurityGroup(acl.getName(), acl.getDescription(), vpcId);
    }

    /**
     * Creates a security group with the given name. If a vpcId is provided then an EC2-VPC security group is created.
     * 
     * @param name
     *            The name of the security group.
     * @param desc
     *            The description of the security group. Optional, may be null.
     * @param vpcId
     *            For EC2-VPC security groups, the id of the VPC the group is for. Optional, may be null.
     * @return A CreateSecurityGroupResponseType that contains the id of the new group.
     */
    public Promise<CreateSecurityGroupResponseType> createSecurityGroup(String name, String desc, String vpcId)
    {
        if (!AWSUtil.isValued(name)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupName")));
        }
        QueryParams params = _connection.initQueryParams(EC2_CREATE_SECURITY_GROUP);
        params.add(new QueryParam("GroupName", name));
        params.add(new QueryParam("GroupDescription", desc != null ? desc : name));
        if (AWSUtil.isValued(vpcId))
            params.add(new QueryParam("VpcId", vpcId));
        Promise<CreateSecurityGroupResponseType> result = _connection.execute(params, CreateSecurityGroupResponseType.class);
        return result;
    }

    /**
     * Method that calls the authorizeSecurityGroupIngress Query API call. See the amazon docs for more info.
     * https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_AuthorizeSecurityGroupIngress.html
     * 
     * @param groupId
     *            The id of the security group to add the rule to.
     * @param perm
     *            IpPermissionType with the permissions to set up.
     * @return An AuthorizeSecurityGroupIngressResponseType. If isReturn is true then it was a success.
     */
    public Promise<AuthorizeSecurityGroupIngressResponseType> authorizeSecurityGroupIngress(final String groupId,
            final IpPermissionType perm)
    {
        if (!AWSUtil.isValued(groupId)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        if (perm == null) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingPermType")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + groupId + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<AuthorizeSecurityGroupIngressResponseType>>() {

            @Override
            public Promise<AuthorizeSecurityGroupIngressResponseType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_AUTHORIZE_SECURITY_GROUP_INGRESS);
                    params.add(new QueryParam("GroupId", groupId));
                    addIngressParams(perm, params);

                    Promise<AuthorizeSecurityGroupIngressResponseType> result =
                            _connection.execute(params, AuthorizeSecurityGroupIngressResponseType.class);
                    result.onComplete(new Callback<AuthorizeSecurityGroupIngressResponseType>() {

                        @Override
                        public void invoke(AuthorizeSecurityGroupIngressResponseType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });
    }

    /**
     * Method that calls the authorizeSecurityGroupEgress Query API call. See the amazon docs for more info.
     * https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_AuthorizeSecurityGroupEgress.html
     * 
     * @param groupId
     *            The id of the security group to add the rule to.
     * @param perm
     *            IpPermissionType with the permissions to set up.
     * @return An AuthorizeSecurityGroupEgressResponseType. If isReturn is true then it was a success.
     */
    public Promise<AuthorizeSecurityGroupEgressResponseType> authorizeSecurityGroupEgress(final String groupId,
            final IpPermissionType perm)
    {
        if (!AWSUtil.isValued(groupId)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        if (perm == null) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingPermType")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + groupId + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<AuthorizeSecurityGroupEgressResponseType>>() {

            @Override
            public Promise<AuthorizeSecurityGroupEgressResponseType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_AUTHORIZE_SECURITY_GROUP_EGRESS);
                    params.add(new QueryParam("GroupId", groupId));
                    addEgressParams(perm, params);

                    Promise<AuthorizeSecurityGroupEgressResponseType> result =
                            _connection.execute(params, AuthorizeSecurityGroupEgressResponseType.class);
                    result.onComplete(new Callback<AuthorizeSecurityGroupEgressResponseType>() {

                        @Override
                        public void invoke(AuthorizeSecurityGroupEgressResponseType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });
    }

    /**
     * Method that calls the revokeSecurityGroupIngress Query API call. See the amazon docs for more info.
     * https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_RevokeSecurityGroupIngress.html
     * 
     * @param groupId
     *            The id of the security group to add the rule to.
     * @param perm
     *            IpPermissionType with the permissions to set up.
     * @return RevokeSecurityGroupIngressResponseType. If isReturn is true then it was a success.
     */
    public Promise<RevokeSecurityGroupIngressResponseType> revokeSecurityGroupIngress(final String groupId,
            final IpPermissionType perm)
    {
        if (!AWSUtil.isValued(groupId)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        if (perm == null) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingPermType")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + groupId + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<RevokeSecurityGroupIngressResponseType>>() {

            @Override
            public Promise<RevokeSecurityGroupIngressResponseType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_REVOKE_SECURITY_GROUP_INGRESS);
                    params.add(new QueryParam("GroupId", groupId));
                    addIngressParams(perm, params);

                    Promise<RevokeSecurityGroupIngressResponseType> result =
                            _connection.execute(params, RevokeSecurityGroupIngressResponseType.class);
                    result.onComplete(new Callback<RevokeSecurityGroupIngressResponseType>() {

                        @Override
                        public void invoke(RevokeSecurityGroupIngressResponseType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });
    }

    /**
     * Method that calls the revokeSecurityGroupEgress Query API call. See the amazon docs for more info.
     * https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_RevokeSecurityGroupEgress.html
     * 
     * @param groupId
     *            The id of the security group to add the rule to.
     * @param perm
     *            IpPermissionType with the permissions to set up.
     * @return RevokeSecurityGroupEngressResponseType. If isReturn is true then it was a success.
     */
    public Promise<RevokeSecurityGroupEgressResponseType> revokeSecurityGroupEgress(final String groupId,
            final IpPermissionType perm)
    {
        if (!AWSUtil.isValued(groupId)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        if (perm == null) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingPermType")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + groupId + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<RevokeSecurityGroupEgressResponseType>>() {

            @Override
            public Promise<RevokeSecurityGroupEgressResponseType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_REVOKE_SECURITY_GROUP_EGRESS);
                    params.add(new QueryParam("GroupId", groupId));
                    addEgressParams(perm, params);

                    Promise<RevokeSecurityGroupEgressResponseType> result =
                            _connection.execute(params, RevokeSecurityGroupEgressResponseType.class);
                    result.onComplete(new Callback<RevokeSecurityGroupEgressResponseType>() {

                        @Override
                        public void invoke(RevokeSecurityGroupEgressResponseType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });
    }

    /**
     * Updates a security group. If the AccessListDirection is Output then the Egress access is updated. Egress rules are only
     * used for EC2-VPC groups and should not be used with EC2-Classic groups. If the AccessListDirection is not Output then the
     * Ingress access is updated. Ingress rules apply to both EC2-Classic and EC2-VPC. Any IpPermissions in the existing security
     * group that don't exist in the AccessList's protocols will be revoked. Any protocols in the AccessList that don't already
     * exist for the security group will be created.
     * 
     * @param acl
     *            AccessList used to set the IpPermissions.
     * @param item
     *            The existing security group to be updated.
     * @return True if the update was a success and false if it wasn't.
     */
    public Promise<Boolean> updateSecurityGroup(final AccessList acl, final SecurityGroupItemType item)
    {
        if (!AWSUtil.isValued(acl)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingAcl")));
        }
        if (!AWSUtil.isValued(item)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroup")));
        }
        if (acl.getDirection() == AccessListDirection.OUTPUT)
            return updateEgress(acl, item);
        else
            return updateIngress(acl, item);
    }

    private Promise<Boolean> updateIngress(final AccessList acl, final SecurityGroupItemType item)
    {
        Map<String, IpPermissionType> existing = new HashMap<String, IpPermissionType>();
        if (item.getIpPermissions() != null) {
            for (IpPermissionType perm : item.getIpPermissions().getItem()) {
                for (IpRangeItemType range : perm.getIpRanges().getItem()) {
                    String key =
                            perm.getIpProtocol() + "-" + perm.getFromPort() + "-" + perm.getToPort() + "-" + range.getCidrIp();
                    existing.put(key, perm);
                }
            }
        }

        List<Promise<?>> promises = new ArrayList<Promise<?>>();
        if (AWSUtil.isValued(acl.getProtocols())) {
            for (Protocol protocol : acl.getProtocols()) {
                if (protocol.isAllowed()) {
                    for (String prefix : protocol.getPrefixes()) {
                        String key =
                                protocol.getProtocol().toLowerCase() + "-" + protocol.getMinPort() + "-" + protocol.getMaxPort()
                                        + "-" + prefix;
                        if (existing.containsKey(key)) {
                            existing.remove(key);
                        }
                        else {
                            IpPermissionType perm = new IpPermissionType();
                            perm.setFromPort(protocol.getMinPort());
                            perm.setToPort(protocol.getMaxPort());
                            perm.setIpProtocol(protocol.getProtocol());
                            IpRangeSetType ips = new IpRangeSetType();
                            IpRangeItemType cidr = new IpRangeItemType();
                            cidr.setCidrIp(prefix);
                            ips.getItem().add(cidr);
                            perm.setIpRanges(ips);
                            promises.add(authorizeSecurityGroupIngress(item.getGroupId(), perm));
                        }
                    }
                }
            }
        }

        // clean up anything left
        for (IpPermissionType perm : existing.values()) {
            promises.add(revokeSecurityGroupIngress(item.getGroupId(), perm));
        }

        // wait for all to complete
        return Promise.sequenceAny(promises).flatMap(new Function<List<Object>, Promise<Boolean>>() {

            @Override
            public Promise<Boolean> invoke(List<Object> response)
            {
                for (Object o : response) {
                    if (o instanceof RevokeSecurityGroupIngressResponseType) {
                        RevokeSecurityGroupIngressResponseType r = (RevokeSecurityGroupIngressResponseType) o;
                        if (!r.isReturn()) {
                            return Promise.pure(false);
                        }
                    }
                    if (o instanceof AuthorizeSecurityGroupIngressResponseType) {
                        AuthorizeSecurityGroupIngressResponseType a = (AuthorizeSecurityGroupIngressResponseType) o;
                        if (!a.isReturn()) {
                            return Promise.pure(false);
                        }
                    }
                }
                return Promise.pure(true);
            }

        });
    }

    private Promise<Boolean> updateEgress(final AccessList acl, final SecurityGroupItemType item)
    {
        Map<String, IpPermissionType> existing = new HashMap<String, IpPermissionType>();
        if (item.getIpPermissionsEgress() != null) {
            for (IpPermissionType perm : item.getIpPermissionsEgress().getItem()) {
                for (IpRangeItemType range : perm.getIpRanges().getItem()) {
                    if (isDefaultRule(perm, range.getCidrIp())) {
                        continue; //don't try to delete the default rule.
                    }
                    String key =
                            perm.getIpProtocol() + "-" + perm.getFromPort() + "-" + perm.getToPort() + "-" + range.getCidrIp();
                    existing.put(key, perm);
                }
            }
        }

        List<Promise<?>> promises = new ArrayList<Promise<?>>();
        if (AWSUtil.isValued(acl.getProtocols())) {
            for (Protocol protocol : acl.getProtocols()) {
                if (protocol.isAllowed()) {
                    for (String prefix : protocol.getPrefixes()) {
                        String key =
                                protocol.getProtocol().toLowerCase() + "-" + protocol.getMinPort() + "-" + protocol.getMaxPort()
                                        + "-" + prefix;
                        if (existing.containsKey(key)) {
                            existing.remove(key);
                        }
                        else {
                            IpPermissionType perm = new IpPermissionType();
                            perm.setFromPort(protocol.getMinPort());
                            perm.setToPort(protocol.getMaxPort());
                            perm.setIpProtocol(protocol.getProtocol());
                            IpRangeSetType ips = new IpRangeSetType();
                            IpRangeItemType cidr = new IpRangeItemType();
                            cidr.setCidrIp(prefix);
                            ips.getItem().add(cidr);
                            perm.setIpRanges(ips);
                            promises.add(authorizeSecurityGroupEgress(item.getGroupId(), perm));
                        }
                    }
                }
            }
        }

        // clean up anything left
        for (IpPermissionType perm : existing.values()) {
            promises.add(revokeSecurityGroupEgress(item.getGroupId(), perm));
        }

        // wait for all to complete
        return Promise.sequenceAny(promises).flatMap(new Function<List<Object>, Promise<Boolean>>() {

            @Override
            public Promise<Boolean> invoke(List<Object> response)
            {
                for (Object o : response) {
                    if (o instanceof RevokeSecurityGroupEgressResponseType) {
                        RevokeSecurityGroupEgressResponseType r = (RevokeSecurityGroupEgressResponseType) o;
                        if (!r.isReturn()) {
                            return Promise.pure(false);
                        }
                    }
                    if (o instanceof AuthorizeSecurityGroupEgressResponseType) {
                        AuthorizeSecurityGroupEgressResponseType a = (AuthorizeSecurityGroupEgressResponseType) o;
                        if (!a.isReturn()) {
                            return Promise.pure(false);
                        }
                    }
                }
                return Promise.pure(true);
            }

        });
    }

    /**
     * Gets the security group for the specified id.
     * 
     * @param sg_id
     *            The id of the desired security group.
     * @return The matching security group.
     */
    public Promise<SecurityGroupItemType> getSecurityGroup(final String sg_id)
    {
        if (!AWSUtil.isValued(sg_id)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + sg_id + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<SecurityGroupItemType>>() {

            @Override
            public Promise<SecurityGroupItemType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_DESCRIBE_SECURITY_GROUPS);
                    params.add(new QueryParam(EC2_CLASSIC_SECURITY_GROUP_ID + ".1", sg_id));
                    Promise<DescribeSecurityGroupsResponseType> promise =
                            _connection.execute(params, DescribeSecurityGroupsResponseType.class);
                    Promise<SecurityGroupItemType> result =
                            promise.map(new Function<DescribeSecurityGroupsResponseType, SecurityGroupItemType>() {

                                @Override
                                public SecurityGroupItemType invoke(DescribeSecurityGroupsResponseType response)
                                {
                                    List<SecurityGroupItemType> matching = response.getSecurityGroupInfo().getItem();
                                    lock.unlock();
                                    return matching.get(0);
                                }

                            });
                    result.onComplete(new Callback<SecurityGroupItemType>() {

                        @Override
                        public void invoke(SecurityGroupItemType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });

    }

    /**
     * Gets the security groups that match the given list of ids.
     * 
     * @see #getSecurityGroup(String)
     * @param sg_ids
     *            A list of security group ids for the desired groups.
     * @return A list of the matching security groups.
     */
    public Promise<List<SecurityGroupItemType>> getSecurityGroups(final List<String> sg_ids)
    {
        if (!AWSUtil.isValued(sg_ids)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupIds")));
        }
        LinkedList<Promise<AsyncLock>> locks = new LinkedList<Promise<AsyncLock>>();
        for (String id : sg_ids) {
            locks.add(AsyncLock.lock("/agility/core/aws/securitygroup/" + id + "/lock"));
        }
        Promise<List<AsyncLock>> locksSeq = Promise.sequence(locks);
        return locksSeq.flatMap(new Function<List<AsyncLock>, Promise<List<SecurityGroupItemType>>>() {

            @Override
            public Promise<List<SecurityGroupItemType>> invoke(final List<AsyncLock> locks)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_DESCRIBE_SECURITY_GROUPS);
                    int i = 1;
                    for (String sg_id : sg_ids)
                        params.add(new QueryParam(EC2_CLASSIC_SECURITY_GROUP_ID + "." + i++, sg_id));
                    Promise<DescribeSecurityGroupsResponseType> promise =
                            _connection.execute(params, DescribeSecurityGroupsResponseType.class);
                    Promise<List<SecurityGroupItemType>> result =
                            promise.map(new Function<DescribeSecurityGroupsResponseType, List<SecurityGroupItemType>>() {

                                @Override
                                public List<SecurityGroupItemType> invoke(DescribeSecurityGroupsResponseType response)
                                {
                                    for (AsyncLock lock : locks) {
                                        lock.unlock();
                                    }
                                    return response.getSecurityGroupInfo().getItem();
                                }

                            });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            for (AsyncLock lock : locks) {
                                lock.unlock();
                            }
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            for (AsyncLock lock : locks) {
                                lock.unlock();
                            }
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    for (AsyncLock lock : locks) {
                        lock.unlock();
                    }
                    return Promise.pure(t);
                }
            }

        });
    }

    /**
     * Deletes the security group with the given id.
     * 
     * @param sg_id
     *            The id of the security group to be deleted.
     * @return DeleteSecurityGroupResponseType If the delete was successful then the _return field in the response object will be
     *         set to True. Otherwise it will be False.
     */
    public Promise<DeleteSecurityGroupResponseType> deleteSecurityGroupById(final String sg_id)
    {
        if (!AWSUtil.isValued(sg_id)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroupId")));
        }
        Promise<AsyncLock> lock = AsyncLock.lock("/agility/core/aws/securitygroup/" + sg_id + "/lock");
        return lock.flatMap(new Function<AsyncLock, Promise<DeleteSecurityGroupResponseType>>() {

            @Override
            public Promise<DeleteSecurityGroupResponseType> invoke(final AsyncLock lock)
            {
                try {
                    QueryParams params = _connection.initQueryParams(EC2_DELETE_SECURITY_GROUP);
                    params.add(new QueryParam(EC2_VPC_SECURITY_GROUP_ID, sg_id));
                    Promise<DeleteSecurityGroupResponseType> result =
                            _connection.execute(params, DeleteSecurityGroupResponseType.class);
                    result.onComplete(new Callback<DeleteSecurityGroupResponseType>() {

                        @Override
                        public void invoke(DeleteSecurityGroupResponseType arg)
                        {
                            lock.unlock();
                        }

                    });
                    result.onFailure(new Callback<Throwable>() {

                        // make sure lock is released if exception is thrown in the future
                        @Override
                        public void invoke(Throwable arg)
                        {
                            lock.unlock();
                        }
                    });
                    result.onCancel(new Callback<Void>() {

                        @Override
                        public void invoke(Void arg0)
                        {
                            lock.unlock();
                        }

                    });
                    return result;
                }
                catch (Throwable t) {
                    // make sure lock is released if exception is thrown during call
                    lock.unlock();
                    return Promise.pure(t);
                }
            }
        });
    }

    /**
     * Deletes the provided security group.
     * 
     * @param sg
     *            The security group to be deleted
     * @return DeleteSecurityGroupResponseType If the delete was successful then the _return field in the response object will be
     *         set to True. Otherwise it will be False.
     */
    public Promise<DeleteSecurityGroupResponseType> deleteSecurityGroup(SecurityGroupItemType sg)
    {
        if (!AWSUtil.isValued(sg)) {
            return Promise.pure(new AWSAdapterException(Resources.getString("missingGroup")));
        }
        return deleteSecurityGroupById(sg.getGroupId());
    }

    //Used to check if the access rule is the default rule applied to all VPC security groups.
    private boolean isDefaultRule(IpPermissionType perm, String CidrIp)
    {
        return perm != null && perm.getToPort() == null && perm.getFromPort() == null && perm.getIpProtocol() != null
                && perm.getIpProtocol().equals("-1") && CidrIp != null && CidrIp.equals("0.0.0.0/0");
    }

    private void addIngressParams(IpPermissionType perm, QueryParams params)
    {
        boolean onlyGroupInfo = true;
        if (AWSUtil.isValued(perm.getFromPort())) {
            params.add(new QueryParam("IpPermissions.1.FromPort", "" + perm.getFromPort()));
            onlyGroupInfo = false;
        }
        if (AWSUtil.isValued(perm.getToPort())) {
            params.add(new QueryParam("IpPermissions.1.ToPort", "" + perm.getToPort()));
            onlyGroupInfo = false;
        }
        if (AWSUtil.isValued(perm.getIpProtocol())) {
            params.add(new QueryParam("IpPermissions.1.IpProtocol", perm.getIpProtocol().toLowerCase()));
            onlyGroupInfo = false;
        }
        if (AWSUtil.isValued(perm.getIpRanges()) && AWSUtil.isValued(perm.getIpRanges().getItem())) {
            int cnt = 1;
            for (IpRangeItemType ipRange : perm.getIpRanges().getItem()) {
                params.add(new QueryParam("IpPermissions.1.IpRanges." + cnt + ".CidrIp", ipRange.getCidrIp()));
                cnt++;
            }
            onlyGroupInfo = false;
        }
        if (AWSUtil.isValued(perm.getGroups()) && AWSUtil.isValued(perm.getGroups().getItem())) {
            int cnt = 1;
            if (onlyGroupInfo) {
                for (UserIdGroupPairType group : perm.getGroups().getItem()) {
                    if (AWSUtil.isValued(group.getGroupName()))
                        params.add(new QueryParam("SourceSecurityGroupName", group.getGroupName()));
                    if (AWSUtil.isValued(group.getUserId()))
                        params.add(new QueryParam("SourceSecurityGroupOwnerId", group.getUserId()));
                    cnt++;
                }
            }
            else {
                for (UserIdGroupPairType group : perm.getGroups().getItem()) {
                    if (AWSUtil.isValued(group.getGroupId()))
                        params.add(new QueryParam("IpPermissions.1.Groups." + cnt + ".GroupId", group.getGroupId()));
                    if (AWSUtil.isValued(group.getUserId()))
                        params.add(new QueryParam("IpPermissions.1.Groups." + cnt + ".UserId", group.getUserId()));
                    if (AWSUtil.isValued(group.getGroupName()))
                        params.add(new QueryParam("IpPermissions.1.Groups." + cnt + ".GroupName", group.getGroupName()));
                    cnt++;
                }
            }
        }
    }

    private void addEgressParams(IpPermissionType perm, QueryParams params)
    {
        if (AWSUtil.isValued(perm.getFromPort())) {
            params.add(new QueryParam("IpPermissions.1.FromPort", "" + perm.getFromPort()));
        }
        if (AWSUtil.isValued(perm.getToPort())) {
            params.add(new QueryParam("IpPermissions.1.ToPort", "" + perm.getToPort()));
        }
        if (AWSUtil.isValued(perm.getIpProtocol())) {
            params.add(new QueryParam("IpPermissions.1.IpProtocol", perm.getIpProtocol().toLowerCase()));
        }
        if (AWSUtil.isValued(perm.getIpRanges()) && AWSUtil.isValued(perm.getIpRanges().getItem())) {
            int cnt = 1;
            for (IpRangeItemType ipRange : perm.getIpRanges().getItem()) {
                params.add(new QueryParam("IpPermissions.1.IpRanges." + cnt + ".CidrIp", ipRange.getCidrIp()));
                cnt++;
            }
        }
        if (AWSUtil.isValued(perm.getGroups()) && AWSUtil.isValued(perm.getGroups().getItem())) {
            int cnt = 1;
            for (UserIdGroupPairType group : perm.getGroups().getItem()) {
                if (AWSUtil.isValued(group.getGroupId()))
                    params.add(new QueryParam("IpPermissions.1.Groups." + cnt + ".GroupId", group.getGroupId()));
                if (AWSUtil.isValued(group.getUserId()))
                    params.add(new QueryParam("IpPermissions.1.Groups." + cnt + ".UserId", group.getUserId()));
                cnt++;
            }
        }
    }

}
