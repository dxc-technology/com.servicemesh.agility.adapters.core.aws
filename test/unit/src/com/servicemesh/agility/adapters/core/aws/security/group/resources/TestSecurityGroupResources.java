package com.servicemesh.agility.adapters.core.aws.security.group.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBElement;

import org.junit.Test;

public class TestSecurityGroupResources {

	@Test
	public void testObjectFactory() {
		ObjectFactory f = new ObjectFactory();
		assertTrue(f.createAuthorizeSecurityGroupEgressResponseType() instanceof AuthorizeSecurityGroupEgressResponseType);
		assertTrue(f.createAuthorizeSecurityGroupIngressResponseType() instanceof AuthorizeSecurityGroupIngressResponseType);
		assertTrue(f.createCreateSecurityGroupResponseType() instanceof CreateSecurityGroupResponseType);
		assertTrue(f.createDeleteSecurityGroupResponseType() instanceof DeleteSecurityGroupResponseType);
		assertTrue(f.createDescribeSecurityGroupsResponseType() instanceof DescribeSecurityGroupsResponseType);
		assertTrue(f.createIpPermissionSetType() instanceof IpPermissionSetType);
		assertTrue(f.createIpPermissionType() instanceof IpPermissionType);
		assertTrue(f.createIpRangeItemType() instanceof IpRangeItemType);
		assertTrue(f.createIpRangeSetType() instanceof IpRangeSetType);
		assertTrue(f.createResourceTagSetItemType() instanceof ResourceTagSetItemType);
		assertTrue(f.createResourceTagSetType() instanceof ResourceTagSetType);
		assertTrue(f.createRevokeSecurityGroupEgressResponseType() instanceof RevokeSecurityGroupEgressResponseType);
		assertTrue(f.createRevokeSecurityGroupIngressResponseType() instanceof RevokeSecurityGroupIngressResponseType);
		assertTrue(f.createSecurityGroupItemType() instanceof SecurityGroupItemType);
		assertTrue(f.createSecurityGroupSetType() instanceof SecurityGroupSetType);
		assertTrue(f.createUserIdGroupPairSetType() instanceof UserIdGroupPairSetType);
		assertTrue(f.createUserIdGroupPairType() instanceof UserIdGroupPairType);
		JAXBElement<AuthorizeSecurityGroupEgressResponseType> e1 = f.createAuthorizeSecurityGroupEgressResponse(new AuthorizeSecurityGroupEgressResponseType());
		assertTrue(e1.getValue() instanceof AuthorizeSecurityGroupEgressResponseType);
		JAXBElement<AuthorizeSecurityGroupIngressResponseType> e2 = f.createAuthorizeSecurityGroupIngressResponse(new AuthorizeSecurityGroupIngressResponseType());
		assertTrue(e2.getValue() instanceof AuthorizeSecurityGroupIngressResponseType);
		JAXBElement<CreateSecurityGroupResponseType> e3 = f.createCreateSecurityGroupResponse(new CreateSecurityGroupResponseType());
		assertTrue(e3.getValue() instanceof CreateSecurityGroupResponseType);
		JAXBElement<DeleteSecurityGroupResponseType> e4 = f.createDeleteSecurityGroupResponse(new DeleteSecurityGroupResponseType());
		assertTrue(e4.getValue() instanceof DeleteSecurityGroupResponseType);
		JAXBElement<DescribeSecurityGroupsResponseType> e5 = f.createDescribeSecurityGroupsResponse(new DescribeSecurityGroupsResponseType());
		assertTrue(e5.getValue() instanceof DescribeSecurityGroupsResponseType);
		JAXBElement<RevokeSecurityGroupEgressResponseType> e6 = f.createRevokeSecurityGroupEgressResponse(new RevokeSecurityGroupEgressResponseType());
		assertTrue(e6.getValue() instanceof RevokeSecurityGroupEgressResponseType);
		JAXBElement<RevokeSecurityGroupIngressResponseType> e7 = f.createRevokeSecurityGroupIngressResponse(new RevokeSecurityGroupIngressResponseType());
		assertTrue(e7.getValue() instanceof RevokeSecurityGroupIngressResponseType);
	}
	
	@Test
	public void testAuthorizeSecurityGroupEgressResponseType() {
		AuthorizeSecurityGroupEgressResponseType a = new AuthorizeSecurityGroupEgressResponseType();
		a.setRequestId("testId");
		a.setReturn(true);
		assertEquals("testId",a.getRequestId());
		assertTrue(a.isReturn());
	}
	
	@Test
	public void testAuthorizeSecurityGroupIngressResponseType() {
		AuthorizeSecurityGroupIngressResponseType a = new AuthorizeSecurityGroupIngressResponseType();
		a.setRequestId("testId");
		a.setReturn(true);
		assertEquals("testId",a.getRequestId());
		assertTrue(a.isReturn());
	}
	
	@Test
	public void testCreateSecurityGroupResponseType() {
		CreateSecurityGroupResponseType c = new CreateSecurityGroupResponseType();
		c.setGroupId("testGroupId");
		c.setRequestId("testId");
		c.setReturn(true);
		assertEquals("testGroupId",c.getGroupId());
		assertEquals("testId",c.getRequestId());
		assertTrue(c.isReturn());
	}
	
	@Test
	public void testDeleteSecurityGroupResponseType() {
		DeleteSecurityGroupResponseType d = new DeleteSecurityGroupResponseType();
		d.setRequestId("testId");
		d.setReturn(true);
		assertEquals("testId",d.getRequestId());
		assertTrue(d.isReturn());
	}
	
	@Test
	public void testDescribeSecurityGroupsResponseType() {
		DescribeSecurityGroupsResponseType d = new DescribeSecurityGroupsResponseType();
		d.setRequestId("testId");
		d.setSecurityGroupInfo(new SecurityGroupSetType());
		assertEquals("testId",d.getRequestId());
		assertTrue(d.getSecurityGroupInfo() instanceof SecurityGroupSetType);
	}
	
	@Test
	public void testIpPermissionSetType() {
		IpPermissionSetType i = new IpPermissionSetType();
		assertEquals(0,i.getItem().size());
		i.getItem().add(new IpPermissionType());
		assertEquals(1,i.getItem().size());
	}
	
	@Test
	public void testIpPermissionType() {
		IpPermissionType i = new IpPermissionType();
		i.setFromPort(new Integer(1));
		i.setGroups(new UserIdGroupPairSetType());
		i.setIpProtocol("tcp");
		i.setIpRanges(new IpRangeSetType());
		i.setToPort(new Integer(2));
		assertEquals(1,i.getFromPort().intValue());
		assertTrue(i.getGroups() instanceof UserIdGroupPairSetType);
		assertEquals("tcp",i.getIpProtocol());
		assertTrue(i.getIpRanges() instanceof IpRangeSetType);
		assertEquals(2,i.getToPort().intValue());
	}
	
	@Test
	public void testIpRangeItemType() {
		IpRangeItemType i = new IpRangeItemType();
		i.setCidrIp("1.1.1.1/32");
		assertEquals("1.1.1.1/32",i.getCidrIp());
	}
	
	@Test
	public void testIpRangeSetType() {
		IpRangeSetType i = new IpRangeSetType();
		assertEquals(0,i.getItem().size());
		i.getItem().add(new IpRangeItemType());
		assertEquals(1,i.getItem().size());
	}
	
	@Test
	public void testResourceTagSetItemType() {
		ResourceTagSetItemType r = new ResourceTagSetItemType();
		r.setKey("testKey");
		r.setValue("testValue");
		assertEquals("testKey",r.getKey());
		assertEquals("testValue", r.getValue());
	}
	
	@Test
	public void testResourceTagSetType() {
		ResourceTagSetType r = new ResourceTagSetType();
		assertTrue(r.getItem().isEmpty());
		r.getItem().add(new ResourceTagSetItemType());
		assertEquals(1,r.getItem().size());
	}
	
	@Test
	public void testRevokeSecurityGroupEgressResponseType() {
		RevokeSecurityGroupEgressResponseType r = new RevokeSecurityGroupEgressResponseType();
		r.setRequestId("testId");
		r.setReturn(true);
		assertEquals("testId", r.getRequestId());
		assertTrue(r.isReturn());
	}
	
	@Test
	public void testRevokeSecurityGroupIngressResponseType() {
		RevokeSecurityGroupIngressResponseType r = new RevokeSecurityGroupIngressResponseType();
		r.setRequestId("testId");
		r.setReturn(true);
		assertEquals("testId", r.getRequestId());
		assertTrue(r.isReturn());
	}
	
	@Test
	public void testSecurityGroupItemType() {
		SecurityGroupItemType s = new SecurityGroupItemType();
		s.setGroupDescription("testDescription");
		s.setGroupId("testId");
		s.setGroupName("testName");
		s.setIpPermissions(new IpPermissionSetType());
		s.setIpPermissionsEgress(new IpPermissionSetType());
		s.setOwnerId("testOwner");
		s.setTagSet(new ResourceTagSetType());
		s.setVpcId("testVPC");
		assertEquals("testDescription",s.getGroupDescription());
		assertEquals("testId",s.getGroupId());
		assertEquals("testName",s.getGroupName());
		assertTrue(s.getIpPermissions() instanceof IpPermissionSetType);
		assertTrue(s.getIpPermissionsEgress() instanceof IpPermissionSetType);
		assertEquals("testOwner",s.getOwnerId());
		assertTrue(s.getTagSet() instanceof ResourceTagSetType);
		assertEquals("testVPC",s.getVpcId());
	}
	
	@Test
	public void testSecurityGroupSetType() {
		SecurityGroupSetType s = new SecurityGroupSetType();
		assertTrue(s.getItem().isEmpty());
		s.getItem().add(new SecurityGroupItemType());
		assertEquals(1,s.getItem().size());	
	}
	
	@Test
	public void testUserIdGroupPairSetType() {
		UserIdGroupPairSetType u = new UserIdGroupPairSetType();
		assertTrue(u.getItem().isEmpty());
		u.getItem().add(new UserIdGroupPairType());
		assertEquals(1,u.getItem().size());	
	}
	
	@Test
	public void testUserIdGroupPairType() {
		UserIdGroupPairType u = new UserIdGroupPairType();
		u.setGroupId("testId");
		u.setGroupName("testName");
		u.setUserId("testUser");
		assertEquals("testId",u.getGroupId());
		assertEquals("testName",u.getGroupName());
		assertEquals("testUser",u.getUserId());
	}
}
