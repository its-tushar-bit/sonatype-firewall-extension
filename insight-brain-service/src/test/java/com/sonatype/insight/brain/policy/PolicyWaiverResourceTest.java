/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyWaiverResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testCRUD_Application()
        throws Exception
    {
        String appPublicId = "PolicyWaiverResourceTest_AppId";
        Application application = createApplication( appPublicId );

        testApplicationCRUD( IdUtils.TYPE_APPLICATION, appPublicId, application.getId() );
    }

    @Test
    public void testCRUD_Organization()
        throws Exception
    {
        Organization organization = createOrganization( "PolicyWaiverResourceTest" );

        testApplicationCRUD( IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId() );
    }

    private void testApplicationCRUD( String ownerType, String ownerPublicId, String ownerId )
        throws Exception
    {
        // Create
        PolicyWaiver policyWaiver =
            new PolicyWaiver( "12345678901234567890", "MyPolicyId", null /* ownerId */, "My comment" );
        Response response =
            RestAccess.post( getServiceURL( ownerType, ownerPublicId ), JsonHelpers.asJson( policyWaiver ) );
        assertResponseStatus( 200, response );
        policyWaiver = JsonHelpers.fromJson( response.getResponseBody(), PolicyWaiver.class );
        assertPolicyWaiver( "MyPolicyId", ownerId, "My comment", policyWaiver );

        // Get
        PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
        List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByOwnerId( ownerId );
        assertEquals( 1, policyWaivers.size() );
        assertPolicyWaiver( "MyPolicyId", ownerId, "My comment", policyWaivers.get( 0 ) );

        // Delete
        policyWaiverDAO.delete( policyWaiver );
    }

    private void assertPolicyWaiver( String policyId, String ownerId, String comment, PolicyWaiver actual )
    {
        assertEquals( policyId, actual.getPolicyId() );
        assertEquals( ownerId, actual.getOwnerId() );
        assertEquals( comment, actual.getComment() );
    }

    private String getServiceURL( final String ownerType, final String ownerId )
    {
        return getRestBaseUrl() + PolicyWaiverResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
    }
}
