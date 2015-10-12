/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */
package com.servicemesh.agility.adapters.core.aws;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.servicemesh.agility.api.AssetProperty;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.Property;

public class TestAWSConfig
{
    @Test
    public void testConfig() throws Exception
    {
        // Constructor
        AWSConfig cfg = new AWSConfig();
        Assert.assertNotNull(cfg);

        // getPropertyAsInteger
        Property p1 = new Property();
        p1.setName("p1");
        p1.setValue("1");

        Property p2 = new Property();
        p2.setName("p2");
        p2.setValue("2");

        List<Property> properties = new ArrayList<Property>();
        properties.add(p1);
        properties.add(p2);

        int i = AWSConfig.getPropertyAsInteger("p1", properties, 3);
        Assert.assertEquals(1, i);
        i = AWSConfig.getPropertyAsInteger("p2", properties, 3);
        Assert.assertEquals(2, i);
        properties.clear();
        i = AWSConfig.getPropertyAsInteger("p2", properties, 3);
        Assert.assertEquals(3, i);
        i = AWSConfig.getPropertyAsInteger("p2", null, 4);
        Assert.assertEquals(4, i);

        // getAssetPropertyAsString
        AssetProperty ap1 = new AssetProperty();
        ap1.setName("ap1");
        ap1.setStringValue(ap1.getName());

        AssetProperty ap2 = new AssetProperty();
        ap2.setName("ap2");
        ap2.setStringValue(ap2.getName());

        List<AssetProperty> aps = new ArrayList<AssetProperty>();
        aps.add(ap1);
        aps.add(ap2);

        String s = AWSConfig.getAssetPropertyAsString("ap1", aps);
        Assert.assertEquals(ap1.getStringValue(), s);
        s = AWSConfig.getAssetPropertyAsString("ap2", aps);
        Assert.assertEquals(ap2.getStringValue(), s);
        s = AWSConfig.getAssetPropertyAsString("ap3", aps);
        Assert.assertNull(s);
        aps.clear();
        s = AWSConfig.getAssetPropertyAsString("ap1", aps);
        Assert.assertNull(s);
        s = AWSConfig.getAssetPropertyAsString("ap1", null);
        Assert.assertNull(s);

        // getAWSCredentials
        Credential cred = AWSConfig.getAWSCredentials(null);
        Assert.assertNull(cred);
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap1.setName(AWSConfig.AWS_ACCESS_KEY);
        ap1.setStringValue(null);
        aps.add(ap1);
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap1.setStringValue("");
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap1.setStringValue("foo");
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap2.setName(AWSConfig.AWS_SECRET_KEY);
        ap2.setStringValue(null);
        aps.add(ap2);
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap2.setStringValue("");
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNull(cred);
        ap2.setStringValue("bar");
        cred = AWSConfig.getAWSCredentials(aps);
        Assert.assertNotNull(cred);
        Assert.assertEquals(ap1.getStringValue(), cred.getPublicKey());
        Assert.assertEquals(ap2.getStringValue(), cred.getPrivateKey());
    }
}
