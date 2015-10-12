/**
 *              Copyright (c) 2008-2013 ServiceMesh, Incorporated; All Rights Reserved
 *              Copyright (c) 2013-Present Computer Sciences Corporation
 */

package com.servicemesh.agility.adapters.core.aws;

import java.util.List;

import com.servicemesh.agility.api.Cloud;
import com.servicemesh.agility.api.Credential;
import com.servicemesh.agility.api.ServiceProvider;

/**
 * Provides a Credential to be used for Amazon Web Services.
 */
public class AWSCredentialFactory
{
    private AWSCredentialFactory()
    {
    }

    private static class Holder
    {
        private static final AWSCredentialFactory _instance = new AWSCredentialFactory();
    }

    /**
     * Gets a credential factory.
     */
    public static AWSCredentialFactory getInstance()
    {
        return Holder._instance;
    }

    /**
     * Gets a Credential with public/private key defined. Preference order being 1) the service provider's direct credentials, 2)
     * the service provider's properties, 3) an associated cloud.
     *
     * @param provider
     *            The service provider
     * @param clouds
     *            Associated clouds
     * @return The credential that was found or null if no credential was found.
     */
    public Credential getCredentials(ServiceProvider provider, List<Cloud> clouds)
    {
        Credential cred = null;
        Credential serviceCandidate = provider.getCredentials();
        if (serviceCandidate != null && serviceCandidate.getPublicKey() != null && serviceCandidate.getPrivateKey() != null) {
            cred = serviceCandidate;
        }

        if (cred == null) {
            cred = AWSConfig.getAWSCredentials(provider.getProperties());
        }

        if ((cred == null) && (provider.getCloud() != null) && (clouds != null)) {
            for (Cloud cloud : clouds) {
                if (cloud.getId() == provider.getCloud().getId()) {
                    Credential cloudCandidate = getCredentials(cloud);
                    if (cloudCandidate != null) {
                        cred = cloudCandidate;
                        break;
                    }
                }
            }
        }
        return cred;
    }

    /**
     * Gets a Credential from the Cloud with public/private key defined.
     *
     * @param cloud
     *            The cloud to be examined
     * @return The credential that was found or null if the public or private key were null.
     */
    public Credential getCredentials(Cloud cloud)
    {
        Credential cred = cloud.getCloudCredentials();
        if ((cred != null) && ((cred.getPublicKey() == null) || (cred.getPrivateKey() == null))) {
            cred = null;
        }
        return cred;
    }
}
